package com.dabsquared.gitlabjenkins.listener;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import javax.annotation.Nonnull;
import javax.ws.rs.WebApplicationException;

/**
 * @author Robin Müller
 */
@Extension
public class GitLabMergeRequestRunListener extends RunListener<Run<?, ?>> {
    @Override
    public void onCompleted(Run<?, ?> build, @Nonnull TaskListener listener) {
        GitLabPushTrigger trigger = GitLabPushTrigger.getFromJob(build.getParent());
        GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);

        if (trigger != null && cause != null && cause.getData().getActionType() == CauseData.ActionType.MERGE) {
            Result buildResult = build.getResult();
            Integer projectId = cause.getData().getProjectId();
            Integer mergeRequestId = cause.getData().getMergeRequestId();
            if (buildResult == Result.SUCCESS) {
                acceptMergeRequestIfNecessary(build, trigger, listener, projectId.toString(), mergeRequestId);
            }
        }
    }

    private void acceptMergeRequestIfNecessary(Run<?, ?> build, GitLabPushTrigger trigger, TaskListener listener, String projectId, Integer mergeRequestId) {
        if (trigger.getAcceptMergeRequestOnSuccess()) {
            try {
                GitLabApi client = getClient(build);
                if (client == null) {
                    listener.getLogger().println("No GitLab connection configured");
                } else {
                    client.acceptMergeRequest(projectId, mergeRequestId, "Merge Request accepted by jenkins build success", false);
                }
            } catch (WebApplicationException e) {
                listener.getLogger().println("Failed to accept merge request.");
            }
        }
    }

    private GitLabApi getClient(Run<?, ?> run) {
        GitLabConnectionProperty connectionProperty = run.getParent().getProperty(GitLabConnectionProperty.class);
        if (connectionProperty != null) {
            return connectionProperty.getClient();
        }
        return null;
    }
}
