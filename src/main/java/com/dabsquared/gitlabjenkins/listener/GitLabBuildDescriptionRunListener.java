package com.dabsquared.gitlabjenkins.listener;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import java.io.IOException;

import static com.dabsquared.gitlabjenkins.util.TriggerUtil.getFromJob;

/**
 * RunListener that will be called when a build starts and completes.
 * Will lookup GitLabPushTrigger and call set the build description if necessary.
 *
 * @author Robin Müller
 */
@Extension
public class GitLabBuildDescriptionRunListener extends RunListener<Run<?, ?>> {

    @Override
    public void onStarted(Run<?, ?> build, TaskListener listener) {
        GitLabPushTrigger trigger = getFromJob(build.getParent(), null);
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
