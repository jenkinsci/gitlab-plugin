package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Branch;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.util.LoggerUtil;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.Job;
import jenkins.model.ParameterizedJobMixIn;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;

/**
 * @author Robin Müller
 */
class OpenMergeRequestPushHookTriggerHandler implements PushHookTriggerHandler {

    private final static Logger LOGGER = Logger.getLogger(OpenMergeRequestPushHookTriggerHandler.class.getName());

    @Override
    public void handle(Job<?, ?> job, PushHook hook, boolean ciSkip, BranchFilter branchFilter) {
        try {
            if (job instanceof AbstractProject<?, ?>) {
                AbstractProject<?, ?> project = (AbstractProject<?, ?>) job;
                GitLabConnectionProperty property = job.getProperty(GitLabConnectionProperty.class);
                final GitLabPushTrigger trigger = project.getTrigger(GitLabPushTrigger.class);
                Integer projectId = hook.getProjectId();
                if (property != null && property.getClient() != null && projectId != null && trigger != null) {
                    GitLabApi client = property.getClient();
                    for (MergeRequest mergeRequest : getOpenMergeRequests(client, projectId.toString())) {
                        handleMergeRequest(job, hook, ciSkip, branchFilter, client, projectId, mergeRequest);
                    }
                }
            }
        } catch (WebApplicationException | ProcessingException e) {
            LOGGER.log(Level.WARNING, "Failed to communicate with gitlab server to determine if this is an update for a merge request: " + e.getMessage(), e);
        }
    }

    private List<MergeRequest> getOpenMergeRequests(GitLabApi client, String projectId) {
        List<MergeRequest> result = new ArrayList<>();
        Integer page = 0;
        do {
            Response response = null;
            try {
                response = client.getMergeRequests(projectId, State.opened, page, 100);
                result.addAll(response.readEntity(new GenericType<List<MergeRequest>>() {}));
                String nextPage = response.getHeaderString("X-Next-Page");
                page = nextPage.isEmpty() ? null : Integer.valueOf(nextPage);
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        } while (page != null);
        return result;
    }

    private void handleMergeRequest(Job<?, ?> job, PushHook hook, boolean ciSkip, BranchFilter branchFilter, GitLabApi client, Integer projectId, MergeRequest mergeRequest) {
        if (ciSkip && mergeRequest.getDescription() != null && mergeRequest.getDescription().contains("[ci-skip]")) {
            LOGGER.log(Level.INFO, "Skipping MR " + mergeRequest.getTitle() + " due to ci-skip.");
            return;
        }
        String targetBranch = mergeRequest.getTargetBranch();
        String sourceBranch = mergeRequest.getSourceBranch();
        if (targetBranch != null && branchFilter.isBranchAllowed(targetBranch) && hook.getRef().endsWith(targetBranch) && sourceBranch != null) {
            LOGGER.log(Level.INFO, "{0} triggered for push to target branch of open merge request #{1}.",
                    LoggerUtil.toArray(job.getFullName(), mergeRequest.getId()));

            Branch branch = client.getBranch(projectId.toString(), sourceBranch);
            Project project = client.getProject(mergeRequest.getSourceProjectId().toString());
            scheduleBuild(job, new CauseAction(new GitLabWebHookCause(retrieveCauseData(hook, project, mergeRequest, branch))));
        }
    }

    private CauseData retrieveCauseData(PushHook hook, Project project, MergeRequest mergeRequest, Branch branch) {
        return causeData()
                .withActionType(CauseData.ActionType.MERGE)
                .withProjectId(hook.getProjectId())
                .withBranch(branch.getName())
                .withSourceBranch(branch.getName())
                .withUserName(branch.getCommit().getAuthorName())
                .withUserEmail(branch.getCommit().getAuthorEmail())
                .withSourceRepoHomepage(project.getWebUrl())
                .withSourceRepoName(project.getName())
                .withSourceRepoUrl(project.getSshUrlToRepo())
                .withSourceRepoSshUrl(project.getSshUrlToRepo())
                .withSourceRepoHttpUrl(project.getHttpUrlToRepo())
                .withMergeRequestTitle(mergeRequest.getTitle())
                .withMergeRequestDescription(mergeRequest.getDescription())
                .withMergeRequestId(mergeRequest.getId())
                .withTargetBranch(mergeRequest.getTargetBranch())
                .withTargetRepoName(hook.getRepository().getName())
                .withTargetRepoSshUrl(hook.getRepository().getGitSshUrl())
                .withTargetRepoHttpUrl(hook.getRepository().getGitHttpUrl())
                .withTriggeredByUser(hook.getCommits().get(0).getAuthor().getName())
                .build();
    }

    private void scheduleBuild(Job<?, ?> job, Action action) {
        int projectBuildDelay = 0;
        if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
            ParameterizedJobMixIn.ParameterizedJob abstractProject = (ParameterizedJobMixIn.ParameterizedJob) job;
            if (abstractProject.getQuietPeriod() > projectBuildDelay) {
                projectBuildDelay = abstractProject.getQuietPeriod();
            }
        }
        retrieveScheduleJob(job).scheduleBuild2(projectBuildDelay, action);
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
}
