package com.dabsquared.gitlabjenkins.listener;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import java.io.IOException;

/**
 * RunListener that will be called when a build starts and completes.
 * Will lookup GitLabPushTrigger and call set the build description if necessary.
 *
 * @author Robin Müller
 */
@Extension
public class GitLabBuildDescriptionRunListener extends RunListener<AbstractBuild<?, ?>> {

    @Override
    public void onStarted(AbstractBuild<?, ?> build, TaskListener listener) {
        GitLabPushTrigger trigger = build.getProject().getTrigger(GitLabPushTrigger.class);
        if (trigger != null && trigger.getSetBuildDescription()) {
            Cause cause = build.getCause(GitLabWebHookCause.class);
            if (cause != null && !cause.getShortDescription().isEmpty()) {
                try {
                    build.setDescription(cause.getShortDescription());
                } catch (IOException e) {
                    listener.getLogger().println("Failed to set build description");
                }
            }
        }
    }

}
