package com.dabsquared.gitlabjenkins.webhook;

import com.dabsquared.gitlabjenkins.util.ACLUtil;
import com.dabsquared.gitlabjenkins.webhook.build.MergeRequestBuildAction;
import com.dabsquared.gitlabjenkins.webhook.build.PushBuildAction;
import com.dabsquared.gitlabjenkins.webhook.status.BranchBuildPageRedirectAction;
import com.dabsquared.gitlabjenkins.webhook.status.BranchStatusPngAction;
import com.dabsquared.gitlabjenkins.webhook.status.CommitBuildPageRedirectAction;
import com.dabsquared.gitlabjenkins.webhook.status.CommitStatusPngAction;
import com.dabsquared.gitlabjenkins.webhook.status.StatusJsonAction;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dabsquared.gitlabjenkins.util.LoggerUtil.toArray;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Robin Müller
 */
public class ActionResolver {

    private static final Logger LOGGER = Logger.getLogger(ActionResolver.class.getName());
    private static final Pattern COMMIT_STATUS_PATTERN =
            Pattern.compile("^(refs/[^/]+/)?(commits|builds)/(?<sha1>[0-9a-fA-F]+)(?<statusJson>/status.json)?$");

    public WebHookAction resolve(final String projectName, StaplerRequest request) {
        Iterator<String> restOfPathParts = Splitter.on('/').omitEmptyStrings().split(request.getRestOfPath()).iterator();
        Job<?, ?> project = resolveProject(projectName, restOfPathParts);
        if (project == null) {
            throw HttpResponses.notFound();
        }
        return resolveAction(project, Joiner.on('/').join(restOfPathParts), request);
    }

    private WebHookAction resolveAction(Job<?, ?> project, String restOfPath, StaplerRequest request) {
        String method = request.getMethod();
        if (method.equals("POST")) {
            return onPost(project, request);
        } else if (method.equals("GET")) {
            return onGet(project, restOfPath, request);
        }
        LOGGER.log(Level.FINE, "Unsupported HTTP method: {0}", method);
        return new NoopAction();
    }

    private WebHookAction onGet(Job<?, ?> project, String restOfPath, StaplerRequest request) {
        Matcher commitMatcher = COMMIT_STATUS_PATTERN.matcher(restOfPath);
        if (restOfPath.isEmpty() && request.hasParameter("ref")) {
            return new BranchBuildPageRedirectAction(project, request.getParameter("ref"));
        } else if (restOfPath.endsWith("status.png")) {
            return onGetStatusPng(project, request);
        } else if (commitMatcher.matches()) {
            return onGetCommitStatus(project, commitMatcher.group("sha1"), commitMatcher.group("statusJson"));
        }
        LOGGER.log(Level.FINE, "Unknown GET request: {0}", restOfPath);
        return new NoopAction();
    }

    private WebHookAction onGetCommitStatus(Job<?, ?> project, String sha1, String statusJson) {
        if (statusJson == null) {
            return new CommitBuildPageRedirectAction(project, sha1);
        } else {
            return new StatusJsonAction(project, sha1);
        }
    }

    private WebHookAction onGetStatusPng(Job<?, ?> project, StaplerRequest request) {
        if (request.hasParameter("ref")) {
            return new BranchStatusPngAction(project, request.getParameter("ref"));
        } else {
            return new CommitStatusPngAction(project, request.getParameter("sha1"));
        }
    }

    private WebHookAction onPost(Job<?, ?> project, StaplerRequest request) {
        String requestBody = getRequestBody(request);
        String eventHeader = request.getHeader("X-Gitlab-Event");
        if (StringUtils.equals(eventHeader, "Merge Request Hook") ) {
            return new MergeRequestBuildAction(project, requestBody);
        } else if ( StringUtils.equals(eventHeader,"Push Hook") || StringUtils.equals(eventHeader,"Tag Push Hook")) {
            return new PushBuildAction(project, requestBody);
        }
        LOGGER.log(Level.FINE, "Unsupported event header: {0}", eventHeader);
        return new NoopAction();
    }

    private String getRequestBody(StaplerRequest request) {
        String requestBody;
        try {
            Charset charset = request.getCharacterEncoding() == null ?  UTF_8 : Charset.forName(request.getCharacterEncoding());
            requestBody = IOUtils.toString(request.getInputStream(), charset);
        } catch (IOException e) {
            throw HttpResponses.error(500, "Failed to read request body");
        }
        return requestBody;
    }

    private Job<?, ?> resolveProject(final String projectName, final Iterator<String> restOfPathParts) {
        return ACLUtil.impersonate(ACL.SYSTEM, new ACLUtil.Function<Job<?, ?>>() {
            public Job<?, ?> invoke() {
                final Jenkins jenkins = Jenkins.getInstance();
                if (jenkins != null) {
                    Item item = jenkins.getItemByFullName(projectName);
                    while (item instanceof ItemGroup<?> && !(item instanceof Job<?, ?>) && restOfPathParts.hasNext()) {
                        item = jenkins.getItem(restOfPathParts.next(), (ItemGroup<?>) item);
                    }
                    if (item instanceof Job<?, ?>) {
                        return (Job<?, ?>) item;
                    }
                }
                LOGGER.log(Level.FINE, "No project found: {0}, {1}", toArray(projectName, Joiner.on('/').join(restOfPathParts)));
                return null;
            }
        });
    }

    private static class NoopAction implements WebHookAction {
        public void execute(StaplerResponse response) {
        }
    }
}
