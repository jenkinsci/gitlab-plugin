package com.dabsquared.gitlabjenkins.webhook;

import static com.dabsquared.gitlabjenkins.util.LoggerUtil.toArray;

import com.dabsquared.gitlabjenkins.util.ACLUtil;
import com.dabsquared.gitlabjenkins.webhook.build.MergeRequestBuildAction;
import com.dabsquared.gitlabjenkins.webhook.build.NoteBuildAction;
import com.dabsquared.gitlabjenkins.webhook.build.PipelineBuildAction;
import com.dabsquared.gitlabjenkins.webhook.build.PushBuildAction;
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
import org.gitlab4j.api.systemhooks.MergeRequestSystemHookEvent;
import org.gitlab4j.api.systemhooks.PushSystemHookEvent;
import org.gitlab4j.api.systemhooks.SystemHookListener;
import org.gitlab4j.api.systemhooks.TagPushSystemHookEvent;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.NoteEvent;
import org.gitlab4j.api.webhook.PipelineEvent;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.TagPushEvent;
import org.gitlab4j.api.webhook.WebHookListener;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author Robin Müller
 */
public class ActionResolver implements WebHookListener, SystemHookListener {

    private static final Logger LOGGER = Logger.getLogger(ActionResolver.class.getName());
    private static final Pattern COMMIT_STATUS_PATTERN =
            Pattern.compile("^(refs/[^/]+/)?(commits|builds)/(?<sha1>[0-9a-fA-F]+)(?<statusJson>/status.json)?$");
    private StaplerRequest request;
    private Item project;

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

    private void setRequest(StaplerRequest request) {
        this.request = request;
    }

    private void setProject(Item project) {
        this.project = project;
    }

    public StaplerRequest getRequest() {
        return request;
    }

    public Item getProject() {
        return project;
    }

    private WebHookAction resolveAction(Item project, String restOfPath, StaplerRequest request) {
        setRequest(request);
        setProject(project);
        String method = request.getMethod();
        if (method.equals("GET")) {
            if (project instanceof Job<?, ?>) {
                return onGet((Job<?, ?>) project, restOfPath, request);
            } else {
                LOGGER.log(Level.FINE, "GET is not supported for this project {0}", project.getName());
                return new NoopAction();
            }
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

    @Override
    public void onPushEvent(PushEvent pushEvent) {
        LOGGER.log(Level.FINE, "Push:{0}", pushEvent.toString());
        fireWebHookBuildAction(getProject(), pushEvent, getRequest());
    }

    @Override
    public void onPushEvent(PushSystemHookEvent pushSystemHookEvent) {
        LOGGER.log(Level.FINE, "PushSystemHook:{0}", pushSystemHookEvent.toString());
        fireSystemHookBuildAction(getProject(), pushSystemHookEvent, getRequest());
    }

    @Override
    public void onMergeRequestEvent(MergeRequestEvent mergeRequestEvent) {
        LOGGER.log(Level.FINE, "MergeRequest:{0}", mergeRequestEvent.toString());
        fireWebHookBuildAction(getProject(), mergeRequestEvent, getRequest());
    }

    @Override
    public void onMergeRequestEvent(MergeRequestSystemHookEvent mergeRequestSystemHookEvent) {
        LOGGER.log(Level.FINE, "MergeRequest:{0}", mergeRequestSystemHookEvent.toString());
        fireSystemHookBuildAction(getProject(), mergeRequestSystemHookEvent, getRequest());
    }

    @Override
    public void onNoteEvent(NoteEvent noteEvent) {
        LOGGER.log(Level.FINE, "Note:{0}", noteEvent.toString());
        fireWebHookBuildAction(getProject(), noteEvent, getRequest());
    }

    @Override
    public void onTagPushEvent(TagPushSystemHookEvent tagPushSystemHookEvent) {
        LOGGER.log(Level.FINE, "TagPush:{0}", tagPushSystemHookEvent.toString());
        fireSystemHookBuildAction(getProject(), tagPushSystemHookEvent, getRequest());
    }

    @Override
    public void onTagPushEvent(TagPushEvent tagPushEvent) {
        LOGGER.log(Level.FINE, "TagPush:{0}", tagPushEvent.toString());
        fireWebHookBuildAction(getProject(), tagPushEvent, getRequest());
    }

    @Override
    public void onPipelineEvent(PipelineEvent pipelineEvent) {
        LOGGER.log(Level.FINE, "Pipeline:{0}", pipelineEvent.toString());
        fireWebHookBuildAction(getProject(), pipelineEvent, getRequest());
    }

    String tokenHeader = getRequest().getHeader("X-Gitlab-Token");

    private WebHookAction fireWebHookBuildAction(Item project, PushEvent pushEvent, StaplerRequest request) {
        return new PushBuildAction(project, pushEvent, tokenHeader);
    }

    private WebHookAction fireSystemHookBuildAction(
            Item project, PushSystemHookEvent pushSystemHookEvent, StaplerRequest request) {
        return new PushBuildAction(project, pushSystemHookEvent, tokenHeader);
    }

    private WebHookAction fireWebHookBuildAction(
            Item project, MergeRequestEvent mergeRequestEvent, StaplerRequest request) {
        return new MergeRequestBuildAction(project, mergeRequestEvent, tokenHeader);
    }

    private WebHookAction fireSystemHookBuildAction(
            Item project, MergeRequestSystemHookEvent mergeRequestSystemHookEvent, StaplerRequest request) {
        return new MergeRequestBuildAction(project, mergeRequestSystemHookEvent, tokenHeader);
    }

    private WebHookAction fireWebHookBuildAction(Item project, TagPushEvent tagPushEvent, StaplerRequest request) {
        return new PushBuildAction(project, tagPushEvent, tokenHeader);
    }

    private WebHookAction fireSystemHookBuildAction(
            Item project, TagPushSystemHookEvent tagPushSystemHookEvent, StaplerRequest request) {
        return new PushBuildAction(project, tagPushSystemHookEvent, tokenHeader);
    }

    private WebHookAction fireWebHookBuildAction(Item project, NoteEvent noteEvent, StaplerRequest request) {
        return new NoteBuildAction(project, noteEvent, tokenHeader);
    }

    private WebHookAction fireWebHookBuildAction(Item project, PipelineEvent pipelineEvent, StaplerRequest request) {
        return new PipelineBuildAction(project, pipelineEvent, tokenHeader);
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
