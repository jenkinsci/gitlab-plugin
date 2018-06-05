package com.dabsquared.gitlabjenkins.trigger.handler;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.publisher.GitLabCommitStatusPublisher;
import com.dabsquared.gitlabjenkins.util.LoggerUtil;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Job;
import hudson.model.Queue;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PendingBuildsHandler {

    private static final Logger LOGGER = Logger.getLogger(PendingBuildsHandler.class.getName());

    public void cancelPendingBuilds(Job<?, ?> job, Integer projectId, String branch) {
        Queue queue = Jenkins.getInstance().getQueue();
        for (Queue.Item item : queue.getItems()) {
            if (!job.getName().equals(item.task.getName())) {
                continue;
            }
            GitLabWebHookCause queueItemGitLabWebHookCause = getGitLabWebHookCauseData(item);
            if (queueItemGitLabWebHookCause == null) {
                continue;
            }
            CauseData queueItemCauseData = queueItemGitLabWebHookCause.getData();
            if (!projectId.equals(queueItemCauseData.getSourceProjectId())) {
                continue;
            }
            if (branch.equals(queueItemCauseData.getBranch())) {
                cancel(item, queue, branch);
                setCommitStatusCancelledIfNecessary(queueItemCauseData, job);
            }
        }
    }

    private GitLabWebHookCause getGitLabWebHookCauseData(Queue.Item item) {
        for (Cause cause : item.getCauses()) {
            if (cause instanceof GitLabWebHookCause) {
                return (GitLabWebHookCause) cause;
            }
        }
        return null;
    }

    private void cancel(Queue.Item item, Queue queue, String branch) {
        try {
            LOGGER.log(Level.INFO, "Cancelling job {0} for branch {1}", LoggerUtil.toArray(item.task.getName(), branch));
            queue.cancel(item);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cancelling queued build", e);
        }
    }

    private void setCommitStatusCancelledIfNecessary(CauseData causeData, Job<?, ?> job) {
        String buildName = resolvePendingBuildName(job);
        if (StringUtils.isBlank(buildName)) {
            return;
        }
        String targetUrl = DisplayURLProvider.get().getJobURL(job);
        GitLabClient client = job.getProperty(GitLabConnectionProperty.class).getClient();
        try {
            client.changeBuildStatus(causeData.getSourceProjectId(), causeData.getLastCommit(), BuildState.canceled,
                causeData.getSourceBranch(), buildName, targetUrl, BuildState.canceled.name());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to set build state to pending", e);
        }
    }

    public static String resolvePendingBuildName(Job<?, ?> job) {
        if (job instanceof AbstractProject) {
            GitLabCommitStatusPublisher publisher =
                (GitLabCommitStatusPublisher) ((AbstractProject) job).getPublishersList().get(GitLabCommitStatusPublisher.class);
            if (publisher != null) {
                return publisher.getName();
            }
        } else if (job instanceof WorkflowJob) {
            GitLabPushTrigger trigger = GitLabPushTrigger.getFromJob(job);
            if (trigger != null) {
                return trigger.getPendingBuildName();
            }
        }
        return null;
    }
}
