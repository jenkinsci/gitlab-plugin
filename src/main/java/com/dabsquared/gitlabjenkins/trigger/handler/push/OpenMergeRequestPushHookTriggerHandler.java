package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.GitLabMergeCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Branch;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.api.model.PushHook;
import com.dabsquared.gitlabjenkins.gitlab.api.model.State;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.util.LoggerUtil;
import com.google.common.base.Optional;
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

import static com.dabsquared.gitlabjenkins.gitlab.api.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.builder.generated.MergeRequestHookBuilder.mergeRequestHook;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.builder.generated.ObjectAttributesBuilder.objectAttributes;

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
                Optional<Integer> projectId = hook.optProjectId();
                if (property != null && property.getClient() != null && projectId.isPresent() && trigger != null) {
                    GitLabApi client = property.getClient();
                    for (MergeRequest mergeRequest : getOpenMergeRequests(client, projectId.get().toString())) {
                            handleMergeRequest(job, hook, ciSkip, branchFilter, client, projectId.get(), mergeRequest);
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
        if (ciSkip && mergeRequest.optDescription().or("").contains("[ci-skip]")) {
            LOGGER.log(Level.INFO, "Skipping MR " + mergeRequest.optTitle().or("") + " due to ci-skip.");
            return;
        }
        Optional<String> targetBranch = mergeRequest.optTargetBranch();
        Optional<String> sourceBranch = mergeRequest.optSourceBranch();
        if (targetBranch.isPresent()
                && branchFilter.isBranchAllowed(targetBranch.get())
                && hook.optRef().or("").endsWith(targetBranch.get())
                && sourceBranch.isPresent()) {
            LOGGER.log(Level.INFO, "{0} triggered for push to target branch of open merge request #{1}.",
                    LoggerUtil.toArray(job.getFullName(), mergeRequest.optIid().orNull()));

            Branch branch = client.getBranch(projectId.toString(), sourceBranch.get());
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

    private MergeRequestHook createMergeRequest(Integer projectId, MergeRequest mergeRequest, Branch branch) {
        return mergeRequestHook()
                .withObjectKind("merge_request")
                .withObjectAttributes(objectAttributes()
                        .withAssigneeId(mergeRequest.getAssignee().optId().orNull())
                        .withAuthorId(mergeRequest.getAuthor().optId().orNull())
                        .withDescription(mergeRequest.optDescription().orNull())
                        .withId(mergeRequest.optId().orNull())
                        .withIid(mergeRequest.optIid().orNull())
                        .withMergeStatus(mergeRequest.optMergeStatus().orNull())
                        .withSourceBranch(mergeRequest.optSourceBranch().orNull())
                        .withSourceProjectId(mergeRequest.optSourceProjectId().orNull())
                        .withTargetBranch(mergeRequest.optTargetBranch().orNull())
                        .withTargetProjectId(projectId)
                        .withTitle(mergeRequest.optTitle().orNull())
                        .withLastCommit(commit()
                                .withId(branch.getCommit().optId().orNull())
                                .withMessage(branch.getCommit().optMessage().orNull())
                                .withUrl("/projects/" + projectId + "/repository/commits/" + branch.getCommit().optId().orNull())
                                .build())
                        .build())
                .build();
    }
}
