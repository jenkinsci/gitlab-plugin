/* Note for Reviewers :
 * changebuildstatus() is replaced by addcommitstatus() for updating the commit status.
 */
package com.dabsquared.gitlabjenkins.trigger.handler;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.publisher.GitLabCommitStatusPublisher;
import com.dabsquared.gitlabjenkins.util.LoggerUtil;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Job;
import hudson.model.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.gitlab4j.api.Constants.CommitBuildState;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.CommitStatus;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

public class PendingBuildsHandler {

    private static final Logger LOGGER = Logger.getLogger(PendingBuildsHandler.class.getName());

    public void cancelPendingBuilds(Job<?, ?> job, Long projectId, String branch) {
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
            LOGGER.log(
                    Level.INFO, "Cancelling job {0} for branch {1}", LoggerUtil.toArray(item.task.getName(), branch));
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
        GitLabApi gitLabApi = job.getProperty(GitLabConnectionProperty.class).getGitLabApi();
        String ref = StringUtils.removeStart(causeData.getSourceBranch(), "refs/tags/");
        try {
            CommitStatus status = new CommitStatus();
            status.withRef(ref)
                    .withName(buildName)
                    .withDescription(CommitBuildState.CANCELED.name())
                    .withCoverage(null) // dont know whether it should be null or not
                    .withTargetUrl(targetUrl);
            gitLabApi
                    .getCommitsApi()
                    .addCommitStatus(
                            causeData.getSourceProjectId(),
                            causeData.getLastCommit(),
                            CommitBuildState.CANCELED,
                            status);
        } catch (GitLabApiException e) {
            LOGGER.log(Level.SEVERE, "Failed to set build state to pending", e);
        }
    }

    public static String resolvePendingBuildName(Job<?, ?> job) {
        if (job instanceof AbstractProject) {
            GitLabCommitStatusPublisher publisher = (GitLabCommitStatusPublisher)
                    ((AbstractProject) job).getPublishersList().get(GitLabCommitStatusPublisher.class);
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
