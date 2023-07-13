package com.dabsquared.gitlabjenkins.webhook;

import static com.dabsquared.gitlabjenkins.util.LoggerUtil.toArray;

import com.dabsquared.gitlabjenkins.util.ACLUtil;
import com.dabsquared.gitlabjenkins.webhook.status.BranchBuildPageRedirectAction;
import com.dabsquared.gitlabjenkins.webhook.status.BranchStatusPngAction;
import com.dabsquared.gitlabjenkins.webhook.status.CommitBuildPageRedirectAction;
import com.dabsquared.gitlabjenkins.webhook.status.CommitStatusPngAction;
import com.dabsquared.gitlabjenkins.webhook.status.StatusJsonAction;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import java.util.Arrays;
import java.util.Iterator;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMSourceOwner;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.systemhooks.SystemHookManager;
import org.gitlab4j.api.webhook.WebHookManager;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author Robin Müller
 */
public class ActionResolver {

    private static final Logger LOGGER = Logger.getLogger(ActionResolver.class.getName());
    private static final Pattern COMMIT_STATUS_PATTERN =
            Pattern.compile("^(refs/[^/]+/)?(commits|builds)/(?<sha1>[0-9a-fA-F]+)(?<statusJson>/status.json)?$");
    private Item project;
    private StaplerRequest request;

    public WebHookAction resolve(final String projectName, StaplerRequest request) {
        Iterator<String> restOfPathParts = Arrays.stream(request.getRestOfPath().split("/"))
                .filter(s -> !s.isEmpty())
                .iterator();
        Item project = resolveProject(projectName, restOfPathParts);
        if (project == null) {
            throw HttpResponses.notFound();
        }
        StringJoiner restOfPath = new StringJoiner("/");
        while (restOfPathParts.hasNext()) {
            restOfPath.add(restOfPathParts.next());
        }
        return resolveAction(project, restOfPath.toString(), request);
    }

    private void setProject(Item project) {
        this.project = project;
    }

    private void setRequest(StaplerRequest request) {
        this.request = request;
    }

    public Item getProject() {
        return project;
    }

    public StaplerRequest getRequest() {
        return request;
    }

    private WebHookAction resolveAction(Item project, String restOfPath, StaplerRequest request) {
        setProject(project);
        setRequest(request);
        String method = request.getMethod();
        if (method.equals("GET")) {
            if (project instanceof Job<?, ?>) {
                return onGet((Job<?, ?>) project, restOfPath, request);
            } else {
                LOGGER.log(Level.FINE, "GET is not supported for this project {0}", project.getName());
                return new NoopAction();
            }
        }
        try {
            WebHookManager webHookManager = new WebHookManager();
            webHookManager.addListener(new GitLabHookResolver(project, request));
            webHookManager.handleEvent(request);
            return new NoopAction();
        } catch (GitLabApiException e) {
            LOGGER.log(Level.FINE, "WebHook was not supported for this project {0}", project.getName());
        }
        try {
            SystemHookManager systemHookManager = new SystemHookManager();
            systemHookManager.addListener(new GitLabHookResolver(project, request));
            systemHookManager.handleEvent(request);
            return new NoopAction();
        } catch (GitLabApiException e) {
            LOGGER.log(Level.FINE, "SystemHook was not supported for this project {0}", project.getName());
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

    private Item resolveProject(final String projectName, final Iterator<String> restOfPathParts) {
        return ACLUtil.impersonate(ACL.SYSTEM, new ACLUtil.Function<Item>() {
            public Item invoke() {
                final Jenkins jenkins = Jenkins.getInstance();
                if (jenkins != null) {
                    Item item = jenkins.getItemByFullName(projectName);
                    while (item instanceof ItemGroup<?>
                            && !(item instanceof Job<?, ?> || item instanceof SCMSourceOwner)
                            && restOfPathParts.hasNext()) {
                        item = jenkins.getItem(restOfPathParts.next(), (ItemGroup<?>) item);
                    }
                    if (item instanceof Job<?, ?> || item instanceof SCMSourceOwner) {
                        return item;
                    }
                }
                LOGGER.log(Level.FINE, "No project found: {0}", toArray(projectName));
                return null;
            }
        });
    }

    static class NoopAction implements WebHookAction {
        public void execute(StaplerResponse response) {}
    }
}
