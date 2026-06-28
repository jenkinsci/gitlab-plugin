package com.dabsquared.gitlabjenkins.trigger.handler.pipeline;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;
import static com.dabsquared.gitlabjenkins.trigger.handler.builder.generated.BuildStatusUpdateBuilder.buildStatusUpdate;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PipelineEventObjectAttributes;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PipelineHook;
import com.dabsquared.gitlabjenkins.trigger.exception.NoRevisionToBuildException;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import com.dabsquared.gitlabjenkins.trigger.handler.AbstractWebHookTriggerHandler;
import com.dabsquared.gitlabjenkins.util.BuildUtil;
import com.dabsquared.gitlabjenkins.util.LoggerUtil;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.RevisionParameterAction;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Milena Zachow
 */
class PipelineHookTriggerHandlerImpl extends AbstractWebHookTriggerHandler<PipelineHook>
        implements PipelineHookTriggerHandler {

    private static final Logger LOGGER = Logger.getLogger(PipelineHookTriggerHandlerImpl.class.getName());

    private final List<String> allowedStates;
    private MergeRequest resolvedMergeRequest;

    PipelineHookTriggerHandlerImpl(List<String> allowedStates) {
        this.allowedStates = allowedStates;
    }

    @Override
    public void handle(
            Job<?, ?> job,
            PipelineHook hook,
            boolean ciSkip,
            BranchFilter branchFilter,
            MergeRequestLabelFilter mergeRequestLabelFilter) {
        PipelineEventObjectAttributes objectAttributes = hook.getObjectAttributes();
        try {
            GitLabConnectionProperty property = job.getProperty(GitLabConnectionProperty.class);
            if (property != null && property.getClient() != null) {
                GitLabClient client = property.getClient();

                // Resolve project ID
                if (hook.getProject() != null && hook.getProject().getPathWithNamespace() != null) {
                    try {
                        com.dabsquared.gitlabjenkins.gitlab.api.model.Project projectForName =
                                client.getProject(hook.getProject().getPathWithNamespace());
                        hook.setProjectId(projectForName.getId());
                    } catch (Exception e) {
                        LOGGER.log(
                                Level.WARNING,
                                "Failed to communicate with gitlab server to determine project id: " + e.getMessage(),
                                e);
                    }
                }

                // Resolve merge request for the commit SHA
                if (objectAttributes != null && objectAttributes.getSha() != null) {
                    try {
                        String projectPath = hook.getProject() != null
                                ? hook.getProject().getPathWithNamespace()
                                : (hook.getProjectId() != null ? hook.getProjectId().toString() : null);
                        if (projectPath != null) {
                            List<MergeRequest> mergeRequests =
                                    client.getCommitMergeRequests(projectPath, objectAttributes.getSha());
                            if (mergeRequests != null && !mergeRequests.isEmpty()) {
                                resolvedMergeRequest = mergeRequests.get(0);
                                LOGGER.log(
                                        Level.FINE,
                                        "Resolved merge request IID {0} for commit {1}",
                                        new Object[] {resolvedMergeRequest.getIid(), objectAttributes.getSha()});
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.log(
                                Level.WARNING,
                                "Failed to look up merge request for commit "
                                        + objectAttributes.getSha() + ": " + e.getMessage(),
                                e);
                    }
                }
            }
        } catch (WebApplicationException e) {
            LOGGER.log(
                    Level.WARNING,
                    "Failed to communicate with gitlab server: " + e.getMessage(),
                    e);
        }
        if (allowedStates.contains(objectAttributes.getStatus()) && !isLastAlreadyBuild(job, hook)) {
            if (ciSkip && isCiSkip(hook)) {
                LOGGER.log(Level.INFO, "Skipping due to ci-skip.");
                return;
            }
            // we do not call super here, since we do not want the status to be changed
            // in case of pipeline events that could lead to a deadlock
            String sourceBranch = getSourceBranch(hook);
            String targetBranch = getTargetBranch(hook);
            if (branchFilter.isBranchAllowed(sourceBranch, targetBranch)) {
                LOGGER.log(
                        Level.INFO, "{0} triggered for {1}.", LoggerUtil.toArray(job.getFullName(), getTriggerType()));

                super.scheduleBuild(job, createActions(job, hook));
            } else {
                LOGGER.log(Level.INFO, "branch {0} is not allowed", sourceBranch + " or " + targetBranch);
            }
        }
    }

    @Override
    protected boolean isCiSkip(PipelineHook hook) {
        // we don't get a commit message or suchlike that could contain ci-skip
        return false;
    }

    @Override
    protected String getSourceBranch(PipelineHook hook) {
        return hook.getObjectAttributes().getRef() == null
                ? null
                : hook.getObjectAttributes().getRef().replaceFirst("^refs/heads/", "");
    }

    @Override
    protected String getTargetBranch(PipelineHook hook) {
        // If the pipeline is associated with a merge request, use the MR target branch
        if (hook.getMergeRequest() != null && hook.getMergeRequest().getTargetBranch() != null) {
            return hook.getMergeRequest().getTargetBranch();
        }
        // Otherwise fall back to the project's default branch
        if (hook.getProject() != null && hook.getProject().getDefaultBranch() != null) {
            return hook.getProject().getDefaultBranch();
        }
        // Last resort: use the ref
        return hook.getObjectAttributes().getRef() == null
                ? null
                : hook.getObjectAttributes().getRef().replaceFirst("^refs/heads/", "");
    }

    @Override
    protected String getTriggerType() {
        return "pipeline event";
    }

    @Override
    protected CauseData retrieveCauseData(PipelineHook hook) {
        return causeData()
                .withActionType(CauseData.ActionType.PIPELINE)
                .withSourceProjectId(hook.getProject().getId())
                .withBranch(getSourceBranch(hook) == null ? "" : getSourceBranch(hook))
                .withSourceBranch(getSourceBranch(hook) == null ? "" : getSourceBranch(hook))
                .withUserName(
                        hook.getUser() == null || hook.getUser().getName() == null
                                ? ""
                                : hook.getUser().getName())
                .withUserUsername(
                        hook.getUser() == null || hook.getUser().getUsername() == null
                                ? ""
                                : hook.getUser().getUsername())
                .withUserEmail(
                        hook.getUser() == null || hook.getUser().getEmail() == null
                                ? ""
                                : hook.getUser().getEmail())
                .withSourceRepoHomepage(
                        hook.getProject() == null || hook.getProject().getWebUrl() == null
                                ? ""
                                : hook.getProject().getWebUrl())
                .withSourceRepoName(
                        hook.getProject() == null || hook.getProject().getName() == null
                                ? (hook.getRepository() == null || hook.getRepository().getName() == null
                                        ? ""
                                        : hook.getRepository().getName())
                                : hook.getProject().getName())
                .withSourceNamespace(
                        hook.getProject() == null || hook.getProject().getNamespace() == null
                                ? ""
                                : hook.getProject().getNamespace())
                .withSourceRepoSshUrl(
                        hook.getProject() != null && hook.getProject().getGitSshUrl() != null
                                ? hook.getProject().getGitSshUrl()
                                : (hook.getRepository() == null || hook.getRepository().getGitSshUrl() == null
                                        ? ""
                                        : hook.getRepository().getGitSshUrl()))
                .withSourceRepoHttpUrl(
                        hook.getProject() != null && hook.getProject().getGitHttpUrl() != null
                                ? hook.getProject().getGitHttpUrl()
                                : (hook.getRepository() == null || hook.getRepository().getGitHttpUrl() == null
                                        ? ""
                                        : hook.getRepository().getGitHttpUrl()))
                .withMergeRequestTitle(
                        resolvedMergeRequest != null && resolvedMergeRequest.getTitle() != null
                                ? resolvedMergeRequest.getTitle()
                                : (hook.getMergeRequest() != null && hook.getMergeRequest().getTitle() != null
                                        ? hook.getMergeRequest().getTitle()
                                        : ""))
                .withMergeRequestIid(
                        resolvedMergeRequest != null
                                ? resolvedMergeRequest.getIid()
                                : (hook.getMergeRequest() != null ? hook.getMergeRequest().getIid() : null))
                .withMergeRequestId(
                        resolvedMergeRequest != null
                                ? resolvedMergeRequest.getId()
                                : (hook.getMergeRequest() != null ? hook.getMergeRequest().getId() : null))
                .withTargetProjectId(hook.getProject().getId())
                .withTargetBranch(getTargetBranch(hook) == null ? "" : getTargetBranch(hook))
                .withTargetRepoName("")
                .withTargetNamespace("")
                .withTargetRepoSshUrl("")
                .withTargetRepoHttpUrl("")
                .withLastCommit(hook.getObjectAttributes().getSha())
                .withTriggeredByUser(
                        hook.getUser() == null || hook.getUser().getName() == null
                                ? ""
                                : hook.getUser().getName())
                .withRef(
                        hook.getObjectAttributes().getRef() == null
                                ? ""
                                : hook.getObjectAttributes().getRef())
                .withSha(
                        hook.getObjectAttributes().getSha() == null
                                ? ""
                                : hook.getObjectAttributes().getSha())
                .withBeforeSha(
                        hook.getObjectAttributes().getBeforeSha() == null
                                ? ""
                                : hook.getObjectAttributes().getBeforeSha())
                .withStatus(
                        hook.getObjectAttributes().getStatus() == null
                                ? ""
                                : hook.getObjectAttributes().getStatus())
                .withStages(
                        hook.getObjectAttributes().getStages() == null
                                ? ""
                                : hook.getObjectAttributes().getStages().toString())
                .withCreatedAt(
                        hook.getObjectAttributes().getCreatedAt() == null
                                ? ""
                                : hook.getObjectAttributes().getCreatedAt().toString())
                .withFinishedAt(
                        hook.getObjectAttributes().getFinishedAt() == null
                                ? ""
                                : hook.getObjectAttributes().getFinishedAt().toString())
                .withBuildDuration(String.valueOf(hook.getObjectAttributes().getDuration()))
                .withPipelineId(hook.getObjectAttributes().getId())
                .withPipelineIid(hook.getObjectAttributes().getIid())
                .withPipelineSource(hook.getObjectAttributes().getSource())
                .withPipelineUrl(hook.getObjectAttributes().getUrl())
                .withCommitMessage(
                        hook.getCommit() == null || hook.getCommit().getMessage() == null
                                ? null
                                : hook.getCommit().getMessage())
                .withCommitTitle(
                        hook.getCommit() == null || hook.getCommit().getTitle() == null
                                ? null
                                : hook.getCommit().getTitle())
                .withCommitAuthorName(
                        hook.getCommit() == null
                                        || hook.getCommit().getAuthor() == null
                                        || hook.getCommit().getAuthor().getName() == null
                                ? null
                                : hook.getCommit().getAuthor().getName())
                .withCommitAuthorEmail(
                        hook.getCommit() == null
                                        || hook.getCommit().getAuthor() == null
                                        || hook.getCommit().getAuthor().getEmail() == null
                                ? null
                                : hook.getCommit().getAuthor().getEmail())
                .withCommitUrl(
                        hook.getCommit() == null || hook.getCommit().getUrl() == null
                                ? null
                                : hook.getCommit().getUrl())
                .withProjectWebUrl(
                        hook.getProject() == null || hook.getProject().getWebUrl() == null
                                ? null
                                : hook.getProject().getWebUrl())
                .withProjectPathWithNamespace(
                        hook.getProject() == null || hook.getProject().getPathWithNamespace() == null
                                ? null
                                : hook.getProject().getPathWithNamespace())
                .build();
    }

    @Override
    protected RevisionParameterAction createRevisionParameter(PipelineHook hook, GitSCM gitSCM)
            throws NoRevisionToBuildException {
        return new RevisionParameterAction(retrieveRevisionToBuild(hook), retrieveUrIish(hook));
    }

    @Override
    protected BuildStatusUpdate retrieveBuildStatusUpdate(PipelineHook hook) {
        return buildStatusUpdate()
                .withProjectId(hook.getProject().getId())
                .withSha(hook.getObjectAttributes().getSha())
                .withRef(hook.getObjectAttributes().getRef())
                .build();
    }

    private String retrieveRevisionToBuild(PipelineHook hook) throws NoRevisionToBuildException {
        if (hook.getObjectAttributes() != null && hook.getObjectAttributes().getSha() != null) {
            return hook.getObjectAttributes().getSha();
        } else {
            throw new NoRevisionToBuildException();
        }
    }

    private boolean isLastAlreadyBuild(Job<?, ?> project, PipelineHook hook) {
        PipelineEventObjectAttributes objectAttributes = hook.getObjectAttributes();
        if (objectAttributes != null && objectAttributes.getSha() != null) {
            Run<?, ?> lastBuild = BuildUtil.getBuildBySHA1IncludingMergeBuilds(project, objectAttributes.getSha());
            if (lastBuild != null) {
                LOGGER.log(Level.INFO, "Last commit has already been built in build #" + lastBuild.getNumber());
                return true;
            }
        }
        return false;
    }
}
