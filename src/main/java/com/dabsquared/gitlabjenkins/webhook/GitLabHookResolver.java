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

public class GitLabHookResolver implements WebHookListener, SystemHookListener {
    public static final Logger LOGGER = Logger.getLogger(GitLabHookResolver.class.getName());
    private Item project;
    private StaplerRequest request;

    public GitLabHookResolver(Item project, StaplerRequest request) {
        this.project = project;
        this.request = request;
    }

    @Override
    public void onPushEvent(PushEvent pushEvent) {
        LOGGER.log(Level.FINE, "Push:{0}", pushEvent.toString());
        fireWebHookBuildAction(project, pushEvent);
    }

    @Override
    public void onPushEvent(PushSystemHookEvent pushSystemHookEvent) {
        LOGGER.log(Level.FINE, "PushSystemHook:{0}", pushSystemHookEvent.toString());
        fireSystemHookBuildAction(project, pushSystemHookEvent);
    }

    @Override
    public void onMergeRequestEvent(MergeRequestEvent mergeRequestEvent) {
        LOGGER.log(Level.FINE, "MergeRequest:{0}", mergeRequestEvent.toString());
        fireWebHookBuildAction(project, mergeRequestEvent);
    }

    @Override
    public void onMergeRequestEvent(MergeRequestSystemHookEvent mergeRequestSystemHookEvent) {
        LOGGER.log(Level.FINE, "MergeRequest:{0}", mergeRequestSystemHookEvent.toString());
        fireSystemHookBuildAction(project, mergeRequestSystemHookEvent);
    }

    @Override
    public void onNoteEvent(NoteEvent noteEvent) {
        LOGGER.log(Level.FINE, "Note:{0}", noteEvent.toString());
        fireWebHookBuildAction(project, noteEvent);
    }

    @Override
    public void onTagPushEvent(TagPushSystemHookEvent tagPushSystemHookEvent) {
        LOGGER.log(Level.FINE, "TagPush:{0}", tagPushSystemHookEvent.toString());
        fireSystemHookBuildAction(project, tagPushSystemHookEvent);
    }

    @Override
    public void onTagPushEvent(TagPushEvent tagPushEvent) {
        LOGGER.log(Level.FINE, "TagPush:{0}", tagPushEvent.toString());
        fireWebHookBuildAction(project, tagPushEvent);
    }

    @Override
    public void onPipelineEvent(PipelineEvent pipelineEvent) {
        LOGGER.log(Level.FINE, "Pipeline:{0}", pipelineEvent.toString());
        fireWebHookBuildAction(project, pipelineEvent);
    }

    private WebHookAction fireWebHookBuildAction(Item project, PushEvent pushEvent) {
        return new PushBuildAction(project, pushEvent, request.getHeader("X-GitLab-Token"));
    }

    private WebHookAction fireSystemHookBuildAction(Item project, PushSystemHookEvent pushSystemHookEvent) {
        return new PushBuildAction(project, pushSystemHookEvent, request.getHeader("X-GitLab-Token"));
    }

    private WebHookAction fireWebHookBuildAction(Item project, MergeRequestEvent mergeRequestEvent) {
        return new MergeRequestBuildAction(project, mergeRequestEvent, mergeRequestEvent.getRequestSecretToken());
    }

    private WebHookAction fireSystemHookBuildAction(
            Item project, MergeRequestSystemHookEvent mergeRequestSystemHookEvent) {
        return new MergeRequestBuildAction(project, mergeRequestSystemHookEvent, request.getHeader("X-GitLab-Token"));
    }

    private WebHookAction fireWebHookBuildAction(Item project, TagPushEvent tagPushEvent) {
        return new PushBuildAction(project, tagPushEvent, request.getHeader("X-GitLab-Token"));
    }

    private WebHookAction fireSystemHookBuildAction(Item project, TagPushSystemHookEvent tagPushSystemHookEvent) {
        return new PushBuildAction(project, tagPushSystemHookEvent, request.getHeader("X-GitLab-Token"));
    }

    private WebHookAction fireWebHookBuildAction(Item project, NoteEvent noteEvent) {
        return new NoteBuildAction(project, noteEvent, request.getHeader("X-GitLab-Token"));
    }

    private WebHookAction fireWebHookBuildAction(Item project, PipelineEvent pipelineEvent) {
        return new PipelineBuildAction(project, pipelineEvent, request.getHeader("X-GitLab-Token"));
    }
}
