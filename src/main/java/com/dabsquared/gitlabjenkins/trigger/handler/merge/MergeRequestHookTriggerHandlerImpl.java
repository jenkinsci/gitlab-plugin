package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.cause.GitLabMergeCause;
import com.dabsquared.gitlabjenkins.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.trigger.handler.AbstractWebHookTriggerHandler;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.Job;

import java.io.File;
import java.io.IOException;

/**
 * @author Robin Müller
 */
class MergeRequestHookTriggerHandlerImpl extends AbstractWebHookTriggerHandler<MergeRequestHook> implements MergeRequestHookTriggerHandler {

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    protected boolean isCiSkip(MergeRequestHook hook) {
        return hook.getObjectAttributes().getDescription().contains("[ci-skip]");
    }

    @Override
    protected Action[] createActions(Job<?, ?> job, MergeRequestHook hook) {
        return new Action[] { new CauseAction(createGitLabMergeCause(job, hook)) };
    }

    @Override
    protected String getTargetBranch(MergeRequestHook hook) {
        return hook.getObjectAttributes().getTargetBranch();
    }

    @Override
    protected String getTriggerType() {
        return "merge request";
    }

    private GitLabMergeCause createGitLabMergeCause(Job<?, ?> job, MergeRequestHook mergeRequestHook) {
        try {
            return new GitLabMergeCause(mergeRequestHook, new File(job.getRootDir(), "gitlab-polling.log"));
        } catch (IOException ex) {
            return new GitLabMergeCause(mergeRequestHook);
        }
    }
}
