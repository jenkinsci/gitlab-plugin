package com.dabsquared.gitlabjenkins.trigger.handler.pipeline;


import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PipelineEventObjectAttributes;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PipelineHook;
import com.dabsquared.gitlabjenkins.trigger.exception.NoRevisionToBuildException;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import com.dabsquared.gitlabjenkins.trigger.handler.AbstractWebHookTriggerHandler;
import com.dabsquared.gitlabjenkins.util.BuildUtil;
import com.dabsquared.gitlabjenkins.util.LoggerUtil;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.RevisionParameterAction;

import javax.ws.rs.WebApplicationException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;
import static com.dabsquared.gitlabjenkins.trigger.handler.builder.generated.BuildStatusUpdateBuilder.buildStatusUpdate;

/**
 * @author Milena Zachow
 */
class PipelineHookTriggerHandlerImpl extends AbstractWebHookTriggerHandler<PipelineHook> implements PipelineHookTriggerHandler {

    private static final Logger LOGGER = Logger.getLogger(PipelineHookTriggerHandlerImpl.class.getName());

    private final List<String> allowedStates;

    PipelineHookTriggerHandlerImpl(List<String> allowedStates) {
        this.allowedStates = allowedStates;
    }

    @Override
    public void handle(Job<?, ?> job, PipelineHook hook, boolean ciSkip, BranchFilter branchFilter, MergeRequestLabelFilter mergeRequestLabelFilter) {
        PipelineEventObjectAttributes objectAttributes = hook.getObjectAttributes();
        try {
            if (job instanceof AbstractProject<?, ?>) {
                GitLabConnectionProperty property = job.getProperty(GitLabConnectionProperty.class);

                if (property != null && property.getClient() != null) {
                    GitLabClient client = property.getClient();
                    com.dabsquared.gitlabjenkins.gitlab.api.model.Project projectForName = client.getProject(hook.getProject().getPathWithNamespace());
                    hook.setProjectId(projectForName.getId());
                }
            }
        } catch (WebApplicationException e) {
            LOGGER.log(Level.WARNING, "Failed to communicate with gitlab server to determine project id: " + e.getMessage(), e);
        }
        if (allowedStates.contains(objectAttributes.getStatus()) && !isLastAlreadyBuild(job,hook)) {
            if (ciSkip && isCiSkip(hook)) {
                LOGGER.log(Level.INFO, "Skipping due to ci-skip.");
                return;
            }
            //we do not call super here, since we do not want the status to be changed
            //in case of pipeline events that could lead to a deadlock
            String targetBranch = getTargetBranch(hook);
            if (branchFilter.isBranchAllowed(targetBranch)) {
                LOGGER.log(Level.INFO, "{0} triggered for {1}.", LoggerUtil.toArray(job.getFullName(), getTriggerType()));

                super.scheduleBuild(job, createActions(job, hook));
            } else {
                LOGGER.log(Level.INFO, "branch {0} is not allowed", targetBranch);
            }

        }
    }

    @Override
    protected boolean isCiSkip(PipelineHook hook) {
        //we don't get a commit message or suchlike that could contain ci-skip
        return false;
    }

    @Override
    protected String getTargetBranch(PipelineHook hook) {
        return hook.getObjectAttributes().getRef() == null ? null : hook.getObjectAttributes().getRef().replaceFirst("^refs/heads/", "");
    }

    @Override
    protected String getTriggerType() {
        return "pipeline event";
    }

    @Override
    protected CauseData retrieveCauseData(PipelineHook hook) {
        return causeData()
                .withActionType(CauseData.ActionType.PIPELINE)
                .withSourceProjectId(hook.getProjectId())
                .withBranch(getTargetBranch(hook)==null?"":getTargetBranch(hook))
                .withSourceBranch(getTargetBranch(hook)==null?"":getTargetBranch(hook))
                .withUserName(hook.getUser()==null||hook.getUser().getName()==null?"":hook.getUser().getName())
                .withSourceRepoName(hook.getRepository()==null||hook.getRepository().getName()==null?"":hook.getRepository().getName())
                .withSourceNamespace(hook.getProject()==null||hook.getProject().getNamespace()==null?"":hook.getProject().getNamespace())
                .withSourceRepoSshUrl(hook.getRepository()==null||hook.getRepository().getGitSshUrl()==null?"":hook.getRepository().getGitSshUrl())
                .withSourceRepoHttpUrl(hook.getRepository()==null||hook.getRepository()==null?"":hook.getRepository().getGitHttpUrl())
                .withMergeRequestTitle("")
                .withTargetProjectId(hook.getProjectId())
                .withTargetBranch(getTargetBranch(hook)==null?"":getTargetBranch(hook))
                .withTargetRepoName("")
                .withTargetNamespace("")
                .withTargetRepoSshUrl("")
                .withTargetRepoHttpUrl("")
                .withLastCommit(hook.getObjectAttributes().getSha())
                .withTriggeredByUser(hook.getUser()==null||hook.getUser().getName()==null?"":hook.getUser().getName())
                .withRef(hook.getObjectAttributes().getRef()==null?"":hook.getObjectAttributes().getRef())
                .withSha(hook.getObjectAttributes().getSha()==null?"":hook.getObjectAttributes().getSha())
                .withBeforeSha(hook.getObjectAttributes().getBeforeSha()==null?"":hook.getObjectAttributes().getBeforeSha())
                .withStatus(hook.getObjectAttributes().getStatus()==null?"":hook.getObjectAttributes().getStatus().toString())
                .withStages(hook.getObjectAttributes().getStages()==null?"":hook.getObjectAttributes().getStages().toString())
                .withCreatedAt(hook.getObjectAttributes().getCreatedAt()==null?"":hook.getObjectAttributes().getCreatedAt().toString())
                .withFinishedAt(hook.getObjectAttributes().getFinishedAt()==null?"":hook.getObjectAttributes().getFinishedAt().toString())
                .withBuildDuration(String.valueOf(hook.getObjectAttributes().getDuration()))
                .build();
    }

    @Override
    protected RevisionParameterAction createRevisionParameter(PipelineHook hook, GitSCM gitSCM) throws NoRevisionToBuildException {
        return new RevisionParameterAction(retrieveRevisionToBuild(hook), retrieveUrIish(hook));
    }

    @Override
    protected BuildStatusUpdate retrieveBuildStatusUpdate(PipelineHook hook) {
        return buildStatusUpdate()
            .withProjectId(hook.getProjectId())
            .withSha(hook.getObjectAttributes().getSha())
            .withRef(hook.getObjectAttributes().getRef())
            .build();
    }

    private String retrieveRevisionToBuild(PipelineHook hook) throws NoRevisionToBuildException {
        if (hook.getObjectAttributes() != null
                && hook.getObjectAttributes().getSha() != null) {
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
