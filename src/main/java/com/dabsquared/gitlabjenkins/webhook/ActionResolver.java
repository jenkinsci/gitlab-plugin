package com.dabsquared.gitlabjenkins.webhook;

import static com.dabsquared.gitlabjenkins.util.LoggerUtil.toArray;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.dabsquared.gitlabjenkins.util.ACLUtil;
import com.dabsquared.gitlabjenkins.util.JsonUtil;
import com.dabsquared.gitlabjenkins.webhook.build.MergeRequestBuildAction;
import com.dabsquared.gitlabjenkins.webhook.build.NoteBuildAction;
import com.dabsquared.gitlabjenkins.webhook.build.PipelineBuildAction;
import com.dabsquared.gitlabjenkins.webhook.build.PushBuildAction;
import com.dabsquared.gitlabjenkins.webhook.status.BranchBuildPageRedirectAction;
import com.dabsquared.gitlabjenkins.webhook.status.BranchStatusPngAction;
import com.dabsquared.gitlabjenkins.webhook.status.CommitBuildPageRedirectAction;
import com.dabsquared.gitlabjenkins.webhook.status.CommitStatusPngAction;
import com.dabsquared.gitlabjenkins.webhook.status.StatusJsonAction;
import com.fasterxml.jackson.databind.JsonNode;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMSourceOwner;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

/**
 * @author Robin Müller
 */
public class ActionResolver {

    private static final Logger LOGGER = Logger.getLogger(ActionResolver.class.getName());
    private static final Pattern COMMIT_STATUS_PATTERN =
            Pattern.compile("^(refs/[^/]+/)?(commits|builds)/(?<sha1>[0-9a-fA-F]+)(?<statusJson>/status.json)?$");

    public WebHookAction resolve(final String projectName, StaplerRequest2 request) {
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

    private WebHookAction resolveAction(Item project, String restOfPath, StaplerRequest2 request) {
        String method = request.getMethod();
        if (method.equals("POST")) {
            return onPost(project, request);
        } else if (method.equals("GET")) {
            if (project instanceof Job<?, ?> job) {
                return onGet(job, restOfPath, request);
            } else {
                LOGGER.log(Level.FINE, "GET is not supported for this project {0}", project.getName());
                return new NoopAction();
            }
        }
        LOGGER.log(Level.FINE, "Unsupported HTTP method: {0}", method);
        return new NoopAction();
    }

    private WebHookAction onGet(Job<?, ?> project, String restOfPath, StaplerRequest2 request) {
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

    private WebHookAction onGetStatusPng(Job<?, ?> project, StaplerRequest2 request) {
        if (request.hasParameter("ref")) {
            return new BranchStatusPngAction(project, request.getParameter("ref"));
        } else {
            return new CommitStatusPngAction(project, request.getParameter("sha1"));
        }
    }

    private WebHookAction onPost(Item project, StaplerRequest2 request) {
        String eventHeader = request.getHeader("X-Gitlab-Event");
        if (eventHeader == null) {
            LOGGER.log(Level.FINE, "Missing X-Gitlab-Event header");
            return new NoopAction();
        }
        String tokenHeader = request.getHeader("X-Gitlab-Token");
        switch (eventHeader) {
            case "Merge Request Hook":
                return new MergeRequestBuildAction(project, getRequestBody(request), tokenHeader);
            case "Push Hook":
            case "Tag Push Hook":
                return new PushBuildAction(project, getRequestBody(request), tokenHeader);
            case "Note Hook":
                return new NoteBuildAction(project, getRequestBody(request), tokenHeader);
            case "Pipeline Hook":
                return new PipelineBuildAction(project, getRequestBody(request), tokenHeader);
            case "System Hook":
                return onSystemHook(project, getRequestBody(request), tokenHeader);
            default:
                LOGGER.log(Level.FINE, "Unsupported X-Gitlab-Event header: {0}", eventHeader);
                return new NoopAction();
        }
    }

    private WebHookAction onSystemHook(Item project, String requestBody, String tokenHeader) {
        /*
         * Each Gitlab System Hook request uses the same common Header, so the deterministic transform based on the
         * header value, as seen in onPost, is not possible. Instead we need to peek at the payload to make the
         * determination.
         */
        JsonNode jsonTree = null;
        String objectKind = "";
        try {
            jsonTree = JsonUtil.readTree(requestBody);
            objectKind = jsonTree.path("object_kind").asText("");
        } catch (RuntimeException exception) {
            LOGGER.log(Level.FINE, "Could not extract object_kind from request body.");
        }

        switch (objectKind) {
            case "merge_request":
                return new MergeRequestBuildAction(project, jsonTree, tokenHeader);
            case "tag_push":
            case "push":
                return new PushBuildAction(project, jsonTree, tokenHeader);
            default:
                LOGGER.log(Level.FINE, "Unsupported System Hook event type: {0}", objectKind);
                return new NoopAction();
        }
    }

    private String getRequestBody(StaplerRequest2 request) {
        String requestBody;
        try {
            Charset charset =
                    request.getCharacterEncoding() == null ? UTF_8 : Charset.forName(request.getCharacterEncoding());
            requestBody = IOUtils.toString(request.getInputStream(), charset);
        } catch (IOException e) {
            throw HttpResponses.error(500, "Failed to read request body");
        }
        return requestBody;
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
        public void execute(StaplerResponse2 response) {}
    }
}
