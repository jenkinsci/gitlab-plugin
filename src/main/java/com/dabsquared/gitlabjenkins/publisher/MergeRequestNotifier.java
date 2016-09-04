package com.dabsquared.gitlabjenkins.publisher;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;

import java.io.IOException;

import static com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty.getClient;

/**
 * @author Robin Müller
 */
public abstract class MergeRequestNotifier extends Notifier {
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        GitLabApi client = getClient(build);
        if (client == null) {
            listener.getLogger().println("No GitLab connection configured");
            return true;
        }
        Integer projectId = getProjectId(build);
        Integer mergeRequestId = getMergeRequestId(build);
        if (projectId != null && mergeRequestId != null) {
            perform(build, listener, client, projectId, mergeRequestId);
        }
        return true;
    }

    protected abstract void perform(Run<?, ?> build, TaskListener listener, GitLabApi client, Integer projectId, Integer mergeRequestId);

    Integer getProjectId(Run<?, ?> build) {
        GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);
        return cause == null ? null : cause.getData().getTargetProjectId();
    }

    Integer getMergeRequestId(Run<?, ?> build) {
        GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);
        return cause == null ? null : cause.getData().getMergeRequestId();
    }
}
