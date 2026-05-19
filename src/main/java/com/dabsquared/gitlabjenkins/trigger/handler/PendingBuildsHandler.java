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
import hudson.model.Executor;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import jenkins.model.CauseOfInterruption;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

public class PendingBuildsHandler {

    private static final Logger LOGGER = Logger.getLogger(PendingBuildsHandler.class.getName());

    public void cancelPendingBuilds(Job<?, ?> job, Integer projectId, String branch) {
        Queue queue = Objects.requireNonNull(Jenkins.getInstance()).getQueue();
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

    /**
     * Aborts in-flight builds of {@code job} whose {@link GitLabWebHookCause} matches the given
     * source project id and source branch. Intended to be invoked when a merge request is updated
     * and the user has opted in via {@code cancelRunningBuildsOnUpdate}.
     */
    public void cancelRunningBuilds(Job<?, ?> job, Integer projectId, String sourceBranch) {
        for (Run<?, ?> build : job.getBuilds()) {
            if (!build.isBuilding()) {
                continue;
            }
            GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);
            if (cause == null) {
                continue;
            }
            CauseData causeData = cause.getData();
            if (!projectId.equals(causeData.getSourceProjectId())) {
                continue;
            }
            if (!sourceBranch.equals(causeData.getSourceBranch())) {
                continue;
            }
            stopBuild(build, job.getName(), sourceBranch);
            setCommitStatusCancelledIfNecessary(causeData, job);
        }
    }

    private void stopBuild(Run<?, ?> build, String jobName, String sourceBranch) {
        Executor executor = build.getExecutor();
        if (executor == null) {
            return;
        }
        try {
            LOGGER.log(
                    Level.INFO,
                    "Stopping build {0} of job {1} superseded by merge request update on branch {2}",
                    LoggerUtil.toArray(build.getDisplayName(), jobName, sourceBranch));
            executor.interrupt(Result.ABORTED, new SupersededByMergeRequestUpdate(sourceBranch));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error stopping running build", e);
        }
    }

    /** Surfaced in the build log so users understand why the build was killed. */
    public static final class SupersededByMergeRequestUpdate extends CauseOfInterruption {
        private static final long serialVersionUID = 1L;
        private final String sourceBranch;

        public SupersededByMergeRequestUpdate(String sourceBranch) {
            this.sourceBranch = sourceBranch;
        }

        @Override
        public String getShortDescription() {
            return "Superseded by merge request update on source branch '" + sourceBranch + "'";
        }
    }

    private GitLabWebHookCause getGitLabWebHookCauseData(Queue.Item item) {
        for (Cause cause : item.getCauses()) {
            if (cause instanceof GitLabWebHookCause hookCause) {
                return hookCause;
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
        GitLabClient client = job.getProperty(GitLabConnectionProperty.class).getClient();
        String ref = StringUtils.removeStart(causeData.getSourceBranch(), "refs/tags/");
        try {
            client.changeBuildStatus(
                    causeData.getSourceProjectId(),
                    causeData.getLastCommit(),
                    BuildState.canceled,
                    ref,
                    buildName,
                    targetUrl,
                    BuildState.canceled.name());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to set build state to pending", e);
        }
    }

    public static String resolvePendingBuildName(Job<?, ?> job) {
        if (job instanceof AbstractProject project) {
            GitLabCommitStatusPublisher publisher =
                    (GitLabCommitStatusPublisher) project.getPublishersList().get(GitLabCommitStatusPublisher.class);
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
