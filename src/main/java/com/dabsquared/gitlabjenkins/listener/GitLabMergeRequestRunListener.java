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
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Robin Müller
 */
@Extension
public class GitLabMergeRequestRunListener extends RunListener<Run<?, ?>> {

    private static final Logger LOGGER = Logger.getLogger(GitLabMergeRequestRunListener.class.getName());

    @Override
    public void onCompleted(Run<?, ?> build, @Nonnull TaskListener listener) {
        GitLabPushTrigger trigger = GitLabPushTrigger.getFromJob(build.getParent());
        GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);

        if (trigger != null && cause != null && cause.getData().getActionType() == CauseData.ActionType.MERGE) {
            String buildUrl = getBuildUrl(build);
            Result buildResult = build.getResult();
            Integer projectId = cause.getData().getProjectId();
            Integer mergeRequestId = cause.getData().getMergeRequestId();
            addNoteOnMergeRequestIfNecessary(build, trigger, listener, projectId.toString(), mergeRequestId, build.getParent().getDisplayName(), build.getNumber(),
                buildUrl, getResultIcon(trigger, buildResult), buildResult.color.getDescription());
            if (buildResult == Result.SUCCESS) {

                acceptMergeRequestIfNecessary(build, trigger, listener, projectId.toString(), mergeRequestId);
            }
        }
    }

    private String getBuildUrl(Run<?, ?> build) {
        return Jenkins.getInstance().getRootUrl() + build.getUrl();
    }

    private void acceptMergeRequestIfNecessary(Run<?, ?> build, GitLabPushTrigger trigger, TaskListener listener, String projectId, Integer mergeRequestId) {
        LOGGER.log(Level.INFO, "Processing merge request with project id " + projectId + " and merge request id " + Integer.toString(mergeRequestId));
        final GitLabPushTrigger.AcceptMergeRequestBlock acceptMergeRequest = trigger.getAcceptMergeRequestOnSuccess();
        if (acceptMergeRequest != null) {
            try {
                LOGGER.log(Level.INFO, "Merge Request will be accepted.");
                final GitLabApi client = getClient(build);
                if (client == null) {
                    LOGGER.log(Level.SEVERE, "No GitLab connection configured");
                } else {
                    final boolean removeSourceBranch = trigger.getAcceptMergeRequestOnSuccess().getRemoveSourceBranchAfterMerge();
                    LOGGER.log(Level.INFO, "RemoveSourceBranch flag is {0}.", removeSourceBranch ? "enabled" : "disabled");
                    client.acceptMergeRequest(projectId, mergeRequestId, "Merge Request accepted by Jenkins build success", removeSourceBranch);
                }
            } catch (WebApplicationException | ProcessingException e) {
                listener.getLogger().printf("Failed to accept merge request: %s", e.getMessage());
            }
        } else {
            LOGGER.log(Level.INFO, "Automatic merge request acceptance feature disabled.");
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
            } catch (WebApplicationException | ProcessingException e) {
                listener.getLogger().printf("Failed to add message to merge request: %s", e.getMessage());
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
