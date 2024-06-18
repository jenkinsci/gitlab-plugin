package com.dabsquared.gitlabjenkins.publisher;

import static com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty.getClient;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import java.io.IOException;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.MergeRequest;

/**
 * @author Robin Müller
 */
public abstract class MergeRequestNotifier extends Notifier implements MatrixAggregatable {
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        GitLabApi client = getClient(build);
        if (client == null) {
            listener.getLogger().println("No GitLab connection configured");
            return true;
        }
        try {
            MergeRequest mergeRequest = getMergeRequest(build);
            if (mergeRequest != null) {
                perform(build, listener, client, mergeRequest);
            }
            return true;
        } catch (GitLabApiException e) {
            listener.getLogger().println("Failed to create merge request");
            return false;
        }
    }

    public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
        return new MatrixAggregator(build, launcher, listener) {
            @Override
            public boolean endBuild() throws InterruptedException, IOException {
                perform(build, launcher, listener);
                return super.endBuild();
            }
        };
    }

    protected abstract void perform(
            Run<?, ?> build, TaskListener listener, GitLabApi client, MergeRequest mergeRequest);

    MergeRequest getMergeRequest(Run<?, ?> run) throws GitLabApiException {
        GitLabWebHookCause cause = run.getCause(GitLabWebHookCause.class);
        return cause == null ? null : cause.getData().getMergeRequest(run);
    }
}
