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
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import javax.ws.rs.WebApplicationException;
import java.text.MessageFormat;

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
            String buildUrl = getBuildUrl(build);
            Result buildResult = build.getResult();
            Integer projectId = cause.getData().getProjectId();
            Integer mergeRequestId = cause.getData().getMergeRequestId();
            if (buildResult == Result.SUCCESS) {
                acceptMergeRequestIfNecessary(build, trigger, listener, projectId.toString(), mergeRequestId);
            }
            addNoteOnMergeRequestIfNecessary(build, trigger, listener, projectId.toString(), mergeRequestId, build.getProject().getDisplayName(), build.getNumber(),
                    buildUrl, getResultIcon(trigger, buildResult), buildResult.color.getDescription());
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
            } catch (WebApplicationException e) {
                listener.getLogger().println("Failed to accept merge request.");
            }
        }
    }

    private void addNoteOnMergeRequestIfNecessary(Run<?, ?> build, GitLabPushTrigger trigger, TaskListener listener, String projectId, Integer mergeRequestId,
                                                  String projectName, int buildNumber, String buildUrl, String resultIcon, String statusDescription) {
        if (trigger.getAddNoteOnMergeRequest()) {
            String message = MessageFormat.format("{0} Jenkins Build {1}\n\nResults available at: [Jenkins [{2} #{3}]]({4})", resultIcon,
                    statusDescription, projectName, buildNumber, buildUrl);
            try {
                GitLabApi client = getClient(build);
                if (client == null) {
                    listener.getLogger().println("No GitLab connection configured");
                } else {
                    client.createMergeRequestNote(projectId, mergeRequestId, message);
                }
            } catch (WebApplicationException e) {
                listener.getLogger().println("Failed to add message to merge request.");
            }
        }
    }

    private String getResultIcon(GitLabPushTrigger trigger, Result result) {
        if (result == Result.SUCCESS) {
            return trigger.getAddVoteOnMergeRequest() ? ":+1:" : ":white_check_mark:";
        } else {
            return trigger.getAddVoteOnMergeRequest() ? ":-1:" : ":anguished:";
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
