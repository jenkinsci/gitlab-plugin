package com.dabsquared.gitlabjenkins.trigger.handler.push;


import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Branch;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.publisher.GitLabCommitStatusPublisher;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import com.dabsquared.gitlabjenkins.util.LoggerUtil;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.plugins.git.RevisionParameterAction;
import hudson.triggers.Trigger;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;
import org.eclipse.jgit.transport.URIish;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;
import static com.dabsquared.gitlabjenkins.util.LoggerUtil.toArray;

/**
 * @author Robin Müller
 */
class OpenMergeRequestPushHookTriggerHandler implements PushHookTriggerHandler {

    private final static Logger LOGGER = Logger.getLogger(OpenMergeRequestPushHookTriggerHandler.class.getName());

    private final boolean skipWorkInProgressMergeRequest;

    OpenMergeRequestPushHookTriggerHandler(boolean skipWorkInProgressMergeRequest) {
        this.skipWorkInProgressMergeRequest = skipWorkInProgressMergeRequest;
    }

    @Override
    public void handle(Job<?, ?> job, PushHook hook, boolean ciSkip, BranchFilter branchFilter, MergeRequestLabelFilter mergeRequestLabelFilter) {
    	try {
            if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
                ParameterizedJob project = (ParameterizedJobMixIn.ParameterizedJob) job;
                GitLabConnectionProperty property = job.getProperty(GitLabConnectionProperty.class);
                for (Trigger t : project.getTriggers().values()) {
                	if (t instanceof GitLabPushTrigger) {
                		final GitLabPushTrigger trigger = (GitLabPushTrigger) t;
                        Integer projectId = hook.getProjectId();
                        if (property != null && property.getClient() != null && projectId != null && trigger != null) {
                            GitLabClient client = property.getClient();
                            for (MergeRequest mergeRequest : getOpenMergeRequests(client, projectId.toString())) {
                                if (mergeRequestLabelFilter.isMergeRequestAllowed(mergeRequest.getLabels())) {
                                	handleMergeRequest(job, hook, ciSkip, branchFilter, client, mergeRequest);
                                }
                            }
                        }
                	}
                }
                
            } else {
            	LOGGER.log(Level.FINE, "Not a ParameterizedJob: {0}",LoggerUtil.toArray(job.getClass().getName()));
            }
        } catch (WebApplicationException | ProcessingException e) {
            LOGGER.log(Level.WARNING, "Failed to communicate with gitlab server to determine if this is an update for a merge request: " + e.getMessage(), e);
        }
    }

    private List<MergeRequest> getOpenMergeRequests(GitLabClient client, String projectId) {
        List<MergeRequest> result = new ArrayList<>();
        Integer page = 1;
        do {
            List<MergeRequest> mergeRequests = client.getMergeRequests(projectId, State.opened, page, 100);
            result.addAll(mergeRequests);
            page = mergeRequests.isEmpty() ? null : page + 1;
        } while (page != null);
        return result;
    }

    private void handleMergeRequest(Job<?, ?> job, PushHook hook, boolean ciSkip, BranchFilter branchFilter, GitLabClient client, MergeRequest mergeRequest) {
        if (ciSkip && mergeRequest.getDescription() != null && mergeRequest.getDescription().contains("[ci-skip]")) {
            LOGGER.log(Level.INFO, "Skipping MR " + mergeRequest.getTitle() + " due to ci-skip.");
            return;
        }

        Boolean workInProgress = mergeRequest.getWorkInProgress();
        if (skipWorkInProgressMergeRequest && workInProgress != null && workInProgress) {
            LOGGER.log(Level.INFO, "Skip WIP Merge Request #{0} ({1})", toArray(mergeRequest.getIid(), mergeRequest.getTitle()));
            return;
        }

        String targetBranch = mergeRequest.getTargetBranch();
        String sourceBranch = mergeRequest.getSourceBranch();
        if (targetBranch != null && branchFilter.isBranchAllowed(targetBranch) && hook.getRef().endsWith(targetBranch) && sourceBranch != null) {
            LOGGER.log(Level.INFO, "{0} triggered for push to target branch of open merge request #{1}.",
                    LoggerUtil.toArray(job.getFullName(), mergeRequest.getId()));

            Branch branch = client.getBranch(mergeRequest.getSourceProjectId().toString(), sourceBranch);
            Project project = client.getProject(mergeRequest.getSourceProjectId().toString());
            String commit = branch.getCommit().getId();
            setCommitStatusPendingIfNecessary(job, mergeRequest.getSourceProjectId(), commit, branch.getName());

            List<Action> actions = Arrays.<Action>asList(new CauseAction(new GitLabWebHookCause(retrieveCauseData(hook, project, mergeRequest, branch))),
                                                         new RevisionParameterAction(commit, retrieveUrIish(hook)));
            scheduleBuild(job, actions.toArray(new Action[actions.size()]));
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

    private void setCommitStatusPendingIfNecessary(Job<?, ?> job, Integer projectId, String commit, String ref) {
        if (job instanceof AbstractProject && ((AbstractProject) job).getPublishersList().get(GitLabCommitStatusPublisher.class) != null) {
            GitLabCommitStatusPublisher publisher =
                (GitLabCommitStatusPublisher) ((AbstractProject) job).getPublishersList().get(GitLabCommitStatusPublisher.class);
            GitLabClient client = job.getProperty(GitLabConnectionProperty.class).getClient();
            try {
                String targetUrl = Jenkins.getInstance().getRootUrl() + job.getUrl() + job.getNextBuildNumber() + "/";
                client.changeBuildStatus(projectId, commit, BuildState.pending, ref, publisher.getName(), targetUrl, null);
            } catch (WebApplicationException | ProcessingException e) {
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
