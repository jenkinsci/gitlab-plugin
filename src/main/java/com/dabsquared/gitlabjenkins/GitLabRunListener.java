package com.dabsquared.gitlabjenkins;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.triggers.Trigger;

import javax.annotation.Nonnull;

/**
 * RunListener that will be called when a build starts and completes.
 * Will lookup GitLabPushTrigger and call onStarted and onCompleted methods
 * in order to have access to the build and set properties.
 */
@Extension
public class GitLabRunListener extends RunListener<AbstractBuild> {

    @Override
    public void onCompleted(AbstractBuild abstractBuild, @Nonnull TaskListener listener) {
        GitLabPushTrigger trig = getTrigger(abstractBuild);
        if (trig != null) {
            trig.onCompleted(abstractBuild);
        }
        super.onCompleted(abstractBuild, listener);
    }

    @Override
    public void onStarted(AbstractBuild abstractBuild, TaskListener listener) {
        GitLabPushTrigger trig = getTrigger(abstractBuild);
        if (trig != null) {
            trig.onStarted(abstractBuild);
        }
        super.onStarted(abstractBuild, listener);
    }


    private GitLabPushTrigger getTrigger(AbstractBuild abstractBuild) {
        Trigger trig = abstractBuild.getProject().getTrigger(GitLabPushTrigger.class);
        if (trig != null && trig instanceof GitLabPushTrigger)
            return (GitLabPushTrigger) trig;
        return null;
    }
}
