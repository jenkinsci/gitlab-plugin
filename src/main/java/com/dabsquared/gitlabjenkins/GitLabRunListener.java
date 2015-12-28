package com.dabsquared.gitlabjenkins;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.triggers.Trigger;
import jenkins.model.ParameterizedJobMixIn;

import javax.annotation.Nonnull;

/**
 * RunListener that will be called when a build starts and completes.
 * Will lookup GitLabPushTrigger and call onStarted and onCompleted methods
 * in order to have access to the build and set properties.
 */
@Extension
public class GitLabRunListener extends RunListener<Run> {

    @Override
    public void onCompleted(Run run, @Nonnull TaskListener listener) {
        GitLabPushTrigger trigger = getTrigger(run);
        if (trigger != null) {
            trigger.onCompleted(run);
        }
        super.onCompleted(run, listener);
    }

    @Override
    public void onStarted(Run run, TaskListener listener) {
        GitLabPushTrigger trigger = getTrigger(run);
        GitLabPushTrigger.DescriptorImpl desc = GitLabPushTrigger.getDesc();
        if (desc.getGitlabHostUrl() == null || desc.getGitlabApiToken() == null) {
            listener.fatalError("GitLab host URL or API token are not set. Please check global Jenkins settings.");
            run.setResult(Result.FAILURE);
        }

        if (trigger != null) {
            trigger.onStarted(run);
        }
        super.onStarted(run, listener);
    }

    private GitLabPushTrigger getTrigger(Run run) {
        if (run instanceof AbstractBuild) {
            ParameterizedJobMixIn.ParameterizedJob p = ((AbstractBuild) run).getProject();
            for (Trigger t : p.getTriggers().values()) {
                if (t instanceof GitLabPushTrigger) {
                    return (GitLabPushTrigger) t;
                }
            }
        }

        return null;
    }
}
