package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.GitLabMergeCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.model.PushHook;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.util.LoggerUtil;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.security.ACL;
import jenkins.model.ParameterizedJobMixIn;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabCommit;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.model.builder.generated.MergeRequestHookBuilder.mergeRequestHook;
import static com.dabsquared.gitlabjenkins.model.builder.generated.ObjectAttributesBuilder.objectAttributes;

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
                if (property != null && property.getClient() != null && hook.optProjectId().isPresent() && trigger != null) {
                    GitlabAPI client = property.getClient();
                    Integer projectId = hook.optProjectId().get();
                    for (GitlabMergeRequest mergeRequest : client.getOpenMergeRequests(projectId)) {
                        handleMergeRequest(job, hook, ciSkip, branchFilter, client, projectId, mergeRequest);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to communicate with gitlab server to determine if this is an update for a merge request: " + e.getMessage(), e);
        }
    }

    private void handleMergeRequest(Job<?, ?> job, PushHook hook, boolean ciSkip, BranchFilter branchFilter, GitlabAPI client, Integer projectId, GitlabMergeRequest mergeRequest) throws IOException {
        if (ciSkip && mergeRequest.getDescription().contains("[ci-skip]")) {
            LOGGER.log(Level.INFO, "Skipping MR " + mergeRequest.getTitle() + " due to ci-skip.");
            return;
        }
        String targetBranch = mergeRequest.getTargetBranch();
        if (branchFilter.isBranchAllowed(targetBranch) && hook.optRef().or("").endsWith(targetBranch)) {
            LOGGER.log(Level.INFO, "{0} triggered for push to target branch of open merge request #{1}.",
                    LoggerUtil.toArray(job.getFullName(), mergeRequest.getIid()));
            GitlabBranch branch = client.getBranch(createProject(projectId), mergeRequest.getSourceBranch());
            scheduleBuild(job, new CauseAction(new GitLabMergeCause(createMergeRequest(projectId, mergeRequest, branch))));
        }
    }

    @Override
    public boolean isEnabled() {
        return false;
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

    private GitlabProject createProject(Integer projectId) {
        GitlabProject project = new GitlabProject();
        project.setId(projectId);
        return project;
    }

    private MergeRequestHook createMergeRequest(Integer projectId, GitlabMergeRequest mergeRequest, GitlabBranch branch) {
        return mergeRequestHook()
                .withObjectKind("merge_request")
                .withObjectAttributes(objectAttributes()
                        .withAssigneeId(mergeRequest.getAssignee() == null ? null : mergeRequest.getAssignee().getId())
                        .withAuthorId(mergeRequest.getAuthor().getId())
                        .withDescription(mergeRequest.getDescription())
                        .withId(mergeRequest.getId())
                        .withIid(mergeRequest.getIid())
                        .withMergeStatus(mergeRequest.getState())
                        .withSourceBranch(mergeRequest.getSourceBranch())
                        .withSourceProjectId(mergeRequest.getSourceProjectId())
                        .withTargetBranch(mergeRequest.getTargetBranch())
                        .withTargetProjectId(projectId)
                        .withTitle(mergeRequest.getTitle())
                        .withLastCommit(commit()
                                .withId(branch.getCommit().getId())
                                .withMessage(branch.getCommit().getMessage())
                                .withUrl(GitlabProject.URL + "/" + projectId + "/repository" + GitlabCommit.URL + "/" + branch.getCommit().getId())
                                .build())
                        .build())
                .build();
    }
}
