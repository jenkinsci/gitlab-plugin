package com.dabsquared.gitlabjenkins.listener;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import static com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty.getClient;

/**
 * @author Robin Müller
 */
@Extension
public class GitLabMergeRequestRunListener extends RunListener<Run<?, ?>> {
    @Override
    public void onCompleted(Run<?, ?> build, @Nonnull TaskListener listener) {
        GitLabPushTrigger trigger = GitLabPushTrigger.getFromJob(build.getParent());
        GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);

        if (trigger != null && cause != null && (cause.getData().getActionType() == CauseData.ActionType.MERGE || cause.getData().getActionType() == CauseData.ActionType.NOTE)) {
            String buildUrl = getBuildUrl(build);
            Result buildResult = build.getResult();
            Integer projectId = cause.getData().getTargetProjectId();
            Integer mergeRequestId = cause.getData().getMergeRequestId();
            if (buildResult == Result.SUCCESS) {
                acceptMergeRequestIfNecessary(build, trigger, listener, projectId.toString(), mergeRequestId);
            }
        }
    }

    private String getBuildUrl(Run<?, ?> build) {
        return Jenkins.getInstance().getRootUrl() + build.getUrl();
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
            } catch (WebApplicationException | ProcessingException e) {
                listener.getLogger().printf("Failed to accept merge request: %s", e.getMessage());
            }
        }
    }
}
