package com.dabsquared.gitlabjenkins.trigger.handler.push;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;
import static com.dabsquared.gitlabjenkins.util.LoggerUtil.toArray;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import com.dabsquared.gitlabjenkins.trigger.handler.PendingBuildsHandler;
import com.dabsquared.gitlabjenkins.util.LoggerUtil;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.plugins.git.RevisionParameterAction;
import hudson.triggers.Trigger;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.URIish;
import org.gitlab4j.api.Constants.CommitBuildState;
import org.gitlab4j.api.Constants.MergeRequestState;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.CommitStatus;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Project;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;

/**
 * @author Robin Müller
 */
class OpenMergeRequestPushHookTriggerHandler implements PushHookTriggerHandler {

    private static final Logger LOGGER = Logger.getLogger(OpenMergeRequestPushHookTriggerHandler.class.getName());

    private final boolean skipWorkInProgressMergeRequest;

    OpenMergeRequestPushHookTriggerHandler(boolean skipWorkInProgressMergeRequest) {
        this.skipWorkInProgressMergeRequest = skipWorkInProgressMergeRequest;
    }

    @Override
    public void handle(
            Job<?, ?> job,
            PushHook hook,
            boolean ciSkip,
            BranchFilter branchFilter,
            MergeRequestLabelFilter mergeRequestLabelFilter) {
        try {
            if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
                ParameterizedJob<?, ?> project = (ParameterizedJobMixIn.ParameterizedJob) job;
                GitLabConnectionProperty property = job.getProperty(GitLabConnectionProperty.class);
                Collection<Trigger<?>> triggerList = project.getTriggers().values();
                for (Trigger<?> t : triggerList) {
                    if (t instanceof GitLabPushTrigger) {
                        final GitLabPushTrigger trigger = (GitLabPushTrigger) t;
                        Long projectId = hook.getProjectId();
                        if (property != null && property.getClient() != null && projectId != null && trigger != null) {
                            GitLabApi client = property.getClient();
                            for (MergeRequest mergeRequest :
                                    client.getMergeRequestApi().getMergeRequests(projectId, MergeRequestState.OPENED)) {
                                if (mergeRequestLabelFilter.isMergeRequestAllowed(mergeRequest.getLabels())) {
                                    handleMergeRequest(job, hook, ciSkip, branchFilter, client, mergeRequest);
                                }
                            }
                        }
                    }
                }

            } else {
                LOGGER.log(
                        Level.FINE,
                        "Not a ParameterizedJob: {0}",
                        LoggerUtil.toArray(job.getClass().getName()));
            }
        } catch (GitLabApiException e) {
            LOGGER.log(
                    Level.WARNING,
                    "Failed to communicate with gitlab server to determine if this is an update for a merge request: "
                            + e.getMessage(),
                    e);
        }
    }

    private void handleMergeRequest(
            Job<?, ?> job,
            PushHook hook,
            boolean ciSkip,
            BranchFilter branchFilter,
            GitLabApi client,
            MergeRequest mergeRequest) {
        if (ciSkip
                && mergeRequest.getDescription() != null
                && mergeRequest.getDescription().contains("[ci-skip]")) {
            LOGGER.log(Level.INFO, "Skipping MR " + mergeRequest.getTitle() + " due to ci-skip.");
            return;
        }

        Boolean workInProgress = mergeRequest.getWorkInProgress();
        if (skipWorkInProgressMergeRequest && workInProgress != null && workInProgress) {
            LOGGER.log(
                    Level.INFO,
                    "Skip WIP Merge Request #{0} ({1})",
                    toArray(mergeRequest.getIid(), mergeRequest.getTitle()));
            return;
        }

        String sourceBranch = mergeRequest.getSourceBranch();
        String targetBranch = mergeRequest.getTargetBranch();
        if (targetBranch != null
                && branchFilter.isBranchAllowed(sourceBranch, targetBranch)
                && hook.getRef().equals("refs/heads/" + targetBranch)
                && sourceBranch != null) {
            LOGGER.log(
                    Level.INFO,
                    "{0} triggered for push to target branch of open merge request #{1}.",
                    LoggerUtil.toArray(job.getFullName(), mergeRequest.getId()));

            try {
                Branch branch = client.getRepositoryApi()
                        .getBranch(mergeRequest.getSourceProjectId().toString(), sourceBranch);
                String commit = branch.getCommit().getId();
                Project project = client.getProjectApi().getProject(mergeRequest.getSourceProjectId());
                setCommitStatusPendingIfNecessary(job, mergeRequest.getSourceProjectId(), commit, branch.getName());
                List<Action> actions = Arrays.<Action>asList(
                        new CauseAction(new GitLabWebHookCause(retrieveCauseData(hook, project, mergeRequest, branch))),
                        new RevisionParameterAction(commit, retrieveUrIish(hook)));
                scheduleBuild(job, actions.toArray(new Action[actions.size()]));
            } catch (GitLabApiException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Failed to obtain branch/project information for merge request:" + e.getMessage(),
                        e);
                return;
            }
        }
    }

    private CauseData retrieveCauseData(PushHook hook, Project project, MergeRequest mergeRequest, Branch branch) {
        return causeData()
                .withActionType(CauseData.ActionType.MERGE)
                .withSourceProjectId(mergeRequest.getSourceProjectId())
                .withTargetProjectId(hook.getProjectId())
                .withBranch(branch.getName())
                .withSourceBranch(branch.getName())
                .withUserName(branch.getCommit().getAuthorName())
                .withUserEmail(branch.getCommit().getAuthorEmail())
                .withSourceRepoHomepage(project.getWebUrl())
                .withSourceRepoName(project.getName())
                .withSourceNamespace(project.getNamespace().getPath())
                .withSourceRepoUrl(project.getSshUrlToRepo())
                .withSourceRepoSshUrl(project.getSshUrlToRepo())
                .withSourceRepoHttpUrl(project.getHttpUrlToRepo())
                .withMergeCommitSha(mergeRequest.getSha())
                .withMergeRequestTitle(mergeRequest.getTitle())
                .withMergeRequestDescription(mergeRequest.getDescription())
                .withMergeRequestId(mergeRequest.getId())
                .withMergeRequestIid(mergeRequest.getIid())
                .withMergeRequestTargetProjectId(mergeRequest.getTargetProjectId())
                .withTargetBranch(mergeRequest.getTargetBranch())
                .withTargetRepoName(hook.getRepository().getName())
                .withTargetNamespace(hook.getProject().getNamespace())
                .withTargetRepoSshUrl(hook.getRepository().getGitSshUrl())
                .withTargetRepoHttpUrl(hook.getRepository().getGitHttpUrl())
                .withTriggeredByUser(hook.getCommits().get(0).getAuthor().getName())
                .withLastCommit(branch.getCommit().getId())
                .withTargetProjectUrl(project.getWebUrl())
                .build();
    }

    private void setCommitStatusPendingIfNecessary(Job<?, ?> job, Long projectId, String commit, String ref) {
        String buildName = PendingBuildsHandler.resolvePendingBuildName(job);
        if (StringUtils.isNotBlank(buildName)) {
            GitLabApi client = job.getProperty(GitLabConnectionProperty.class).getClient();
            try {
                String fixedTagRef = StringUtils.removeStart(ref, "refs/tags/");
                String targetUrl = DisplayURLProvider.get().getJobURL(job);
                CommitStatus status = new CommitStatus();
                status.withRef(fixedTagRef)
                        .withName(buildName)
                        .withTargetUrl(targetUrl)
                        .withDescription(CommitBuildState.PENDING.name())
                        .withCoverage(null)
                        .withTargetUrl(targetUrl);
                client.getCommitsApi().addCommitStatus(projectId, commit, CommitBuildState.PENDING, status);
            } catch (GitLabApiException e) {
                LOGGER.log(Level.SEVERE, "Failed to set build state to pending", e);
            }
        }
    }

    private void scheduleBuild(Job<?, ?> job, Action[] actions) {
        int projectBuildDelay = 0;
        if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
            ParameterizedJobMixIn.ParameterizedJob abstractProject = (ParameterizedJobMixIn.ParameterizedJob) job;
            if (abstractProject.getQuietPeriod() > projectBuildDelay) {
                projectBuildDelay = abstractProject.getQuietPeriod();
            }
        }
        retrieveScheduleJob(job).scheduleBuild2(projectBuildDelay, actions);
    }

    private ParameterizedJobMixIn retrieveScheduleJob(final Job<?, ?> job) {
        // TODO 1.621+ use standard method
        return new ParameterizedJobMixIn() {
            @Override
            protected Job asJob() {
                return job;
            }
        };
    }

    private URIish retrieveUrIish(PushHook hook) {
        try {
            if (hook.getRepository() != null) {
                return new URIish(hook.getRepository().getUrl());
            }
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "could not parse URL");
        }
        return null;
    }
}
