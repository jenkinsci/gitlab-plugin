package com.dabsquared.gitlabjenkins.webhook;

import com.dabsquared.gitlabjenkins.webhook.build.MergeRequestBuildAction;
import com.dabsquared.gitlabjenkins.webhook.build.NoteBuildAction;
import com.dabsquared.gitlabjenkins.webhook.build.PipelineBuildAction;
import com.dabsquared.gitlabjenkins.webhook.build.PushBuildAction;
import hudson.model.Item;
import java.util.logging.Level;
import java.util.logging.Logger;
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

public class GitLabHookResolver implements WebHookListener, SystemHookListener {
    public static final Logger LOGGER = Logger.getLogger(GitLabHookResolver.class.getName());
    private Item project;
    private StaplerRequest request;
    private StaplerResponse response;

    public GitLabHookResolver(Item project, StaplerRequest request, StaplerResponse response) {
        this.project = project;
        this.request = request;
        this.response = response;
    }

    @Override
    public void onPushEvent(PushEvent pushEvent) {
        LOGGER.log(Level.FINE, "Push:{0}", pushEvent.toString());
        PushBuildAction pushBuildAction = new PushBuildAction(project, pushEvent, request.getHeader("X-GitLab-Token"));
        pushBuildAction.execute(response);
    }

    @Override
    public void onPushEvent(PushSystemHookEvent pushSystemHookEvent) {
        LOGGER.log(Level.FINE, "PushSystemHook:{0}", pushSystemHookEvent.toString());
        PushBuildAction pushBuildAction = new PushBuildAction(project, pushSystemHookEvent, request.getHeader("X-GitLab-Token"));
        pushBuildAction.execute(response);
    }

    @Override
    public void onMergeRequestEvent(MergeRequestEvent mergeRequestEvent) {
        LOGGER.log(Level.FINE, "MergeRequest:{0}", mergeRequestEvent.toString());
        MergeRequestBuildAction mergeRequestBuildAction = new MergeRequestBuildAction(project, mergeRequestEvent, request.getHeader("X-GitLab-Token"));
        mergeRequestBuildAction.execute(response);
    }

    @Override
    public void onMergeRequestEvent(MergeRequestSystemHookEvent mergeRequestSystemHookEvent) {
        LOGGER.log(Level.FINE, "MergeRequest:{0}", mergeRequestSystemHookEvent.toString());
        MergeRequestBuildAction mergeRequestBuildAction = new MergeRequestBuildAction(project, mergeRequestSystemHookEvent, request.getHeader("X-GitLab-Token"));
        mergeRequestBuildAction.execute(response);
    }

    @Override
    public void onNoteEvent(NoteEvent noteEvent) {
        LOGGER.log(Level.FINE, "Note:{0}", noteEvent.toString());
        NoteBuildAction noteBuildAction = new NoteBuildAction(project, noteEvent, request.getHeader("X-GitLab-Token"));
        noteBuildAction.execute(response);
    }

    @Override
    public void onTagPushEvent(TagPushSystemHookEvent tagPushSystemHookEvent) {
        LOGGER.log(Level.FINE, "TagPush:{0}", tagPushSystemHookEvent.toString());
        PushBuildAction pushBuildAction = new PushBuildAction(project, tagPushSystemHookEvent, request.getHeader("X-GitLab-Token"));
        pushBuildAction.execute(response);
    }

    @Override
    public void onTagPushEvent(TagPushEvent tagPushEvent) {
        LOGGER.log(Level.FINE, "TagPush:{0}", tagPushEvent.toString());
        PushBuildAction pushBuildAction = new PushBuildAction(project, tagPushEvent, request.getHeader("X-GitLab-Token"));
        pushBuildAction.execute(response);
    }

    @Override
    public void onPipelineEvent(PipelineEvent pipelineEvent) {
        LOGGER.log(Level.FINE, "Pipeline:{0}", pipelineEvent.toString());
        PipelineBuildAction pipelineBuildAction = new PipelineBuildAction(project, pipelineEvent, request.getHeader("X-GitLab-Token"));
        pipelineBuildAction.execute(response);
    }
}
