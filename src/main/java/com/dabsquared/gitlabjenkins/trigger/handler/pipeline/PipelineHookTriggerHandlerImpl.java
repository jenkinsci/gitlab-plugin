package com.dabsquared.gitlabjenkins.trigger.handler.pipeline;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;
import static com.dabsquared.gitlabjenkins.trigger.handler.builder.generated.BuildStatusUpdateBuilder.buildStatusUpdate;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
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
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.WebApplicationException;
import org.eclipse.jgit.transport.URIish;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectHook;
import org.gitlab4j.api.webhook.PipelineEvent;
import org.gitlab4j.api.webhook.PipelineEvent.ObjectAttributes;

/**
 * @author Milena Zachow
 */
class PipelineHookTriggerHandlerImpl extends AbstractWebHookTriggerHandler<PipelineEvent>
        implements PipelineHookTriggerHandler {

    private static final Logger LOGGER = Logger.getLogger(PipelineHookTriggerHandlerImpl.class.getName());

    private final List<String> allowedStates;

    private ProjectHook hook;

    PipelineHookTriggerHandlerImpl(List<String> allowedStates) {
        this.allowedStates = allowedStates;
    }

    @Override
    public void handle(
            Job<?, ?> job,
            PipelineEvent event,
            boolean ciSkip,
            BranchFilter branchFilter,
            MergeRequestLabelFilter mergeRequestLabelFilter) {
        ObjectAttributes objectAttributes = event.getObjectAttributes();
        try {
            if (job instanceof AbstractProject<?, ?>) {
                GitLabConnectionProperty property = job.getProperty(GitLabConnectionProperty.class);

                if (property != null && property.getClient() != null) {
                    GitLabApi client = property.getClient();
                    Project projectForName =
                            client.getProjectApi().getProject(event.getProject().getPathWithNamespace());
                    hook = client.getProjectApi()
                            .getHook(
                                    projectForName.getId(),
                                    event.getObjectAttributes().getId());
                }
            }
        } catch (WebApplicationException | GitLabApiException e) {
            LOGGER.log(
                    Level.WARNING,
                    "Failed to communicate with gitlab server to determine project id: " + e.getMessage(),
                    e);
        }
        if (allowedStates.contains(objectAttributes.getStatus()) && !isLastAlreadyBuild(job, event)) {
            if (ciSkip && isCiSkip(event)) {
                LOGGER.log(Level.INFO, "Skipping due to ci-skip.");
                return;
            }
            // we do not call super here, since we do not want the status to be changed
            // in case of pipeline events that could lead to a deadlock
            String sourceBranch = getSourceBranch(event);
            String targetBranch = getTargetBranch(event);
            if (branchFilter.isBranchAllowed(sourceBranch, targetBranch)) {
                LOGGER.log(
                        Level.INFO, "{0} triggered for {1}.", LoggerUtil.toArray(job.getFullName(), getTriggerType()));

                super.scheduleBuild(job, createActions(job, event));
            } else {
                LOGGER.log(Level.INFO, "branch {0} is not allowed", sourceBranch + " or " + targetBranch);
            }
        }
    }

    @Override
    protected boolean isCiSkip(PipelineEvent event) {
        // we don't get a commit message or suchlike that could contain ci-skip
        return false;
    }

    @Override
    protected String getSourceBranch(PipelineEvent event) {
        return event.getObjectAttributes().getRef() == null
                ? null
                : event.getObjectAttributes().getRef().replaceFirst("^refs/heads/", "");
    }

    @Override
    protected String getTargetBranch(PipelineEvent event) {
        return event.getObjectAttributes().getRef() == null
                ? null
                : event.getObjectAttributes().getRef().replaceFirst("^refs/heads/", "");
    }

    @Override
    protected String getTriggerType() {
        return "pipeline event";
    }

    @Override
    protected CauseData retrieveCauseData(PipelineEvent event) {

        return causeData()
                .withActionType(CauseData.ActionType.PIPELINE)
                .withSourceProjectId(event.getProject().getId())
                .withBranch(getTargetBranch(event) == null ? "" : getTargetBranch(event))
                .withSourceBranch(getTargetBranch(event) == null ? "" : getTargetBranch(event))
                .withUserName(
                        event.getUser() == null || event.getUser().getName() == null
                                ? ""
                                : event.getUser().getName())
                .withSourceRepoName(
                        //                        event.getRepository() == null || event.getRepository().getName() ==
                        // null
                        event.getProject().getName() == null
                                ? ""
                                : event.getProject().getName())
                .withSourceNamespace(
                        event.getProject() == null || event.getProject().getNamespace() == null
                                ? ""
                                : event.getProject().getNamespace())
                .withSourceRepoSshUrl(
                        event.getProject() == null || event.getProject().getGitSshUrl() == null
                                ? ""
                                : event.getProject().getGitSshUrl())
                .withSourceRepoHttpUrl(
                        event.getProject() == null || event.getProject().getGitHttpUrl() == null
                                ? ""
                                : event.getProject().getGitHttpUrl())
                .withMergeRequestTitle("")
                .withTargetProjectId(event.getProject().getId())
                .withTargetBranch(getTargetBranch(event) == null ? "" : getTargetBranch(event))
                .withTargetRepoName("")
                .withTargetNamespace("")
                .withTargetRepoSshUrl("")
                .withTargetRepoHttpUrl("")
                .withLastCommit(event.getObjectAttributes().getSha())
                .withTriggeredByUser(
                        event.getUser() == null || event.getUser().getName() == null
                                ? ""
                                : event.getUser().getName())
                .withRef(
                        event.getObjectAttributes().getRef() == null
                                ? ""
                                : event.getObjectAttributes().getRef())
                .withSha(
                        event.getObjectAttributes().getSha() == null
                                ? ""
                                : event.getObjectAttributes().getSha())
                .withBeforeSha(
                        event.getObjectAttributes().getBeforeSha() == null
                                ? ""
                                : event.getObjectAttributes().getBeforeSha())
                .withStatus(
                        event.getObjectAttributes().getStatus() == null
                                ? ""
                                : event.getObjectAttributes().getStatus())
                .withStages(
                        event.getObjectAttributes().getStages() == null
                                ? ""
                                : event.getObjectAttributes().getStages().toString())
                .withCreatedAt(
                        event.getObjectAttributes().getCreatedAt() == null
                                ? ""
                                : event.getObjectAttributes().getCreatedAt().toString())
                .withFinishedAt(
                        event.getObjectAttributes().getFinishedAt() == null
                                ? ""
                                : event.getObjectAttributes().getFinishedAt().toString())
                .withBuildDuration(String.valueOf(event.getObjectAttributes().getDuration()))
                .build();
    }

    @Override
    protected RevisionParameterAction createRevisionParameter(PipelineEvent event, GitSCM gitSCM)
            throws NoRevisionToBuildException {
        return new RevisionParameterAction(retrieveRevisionToBuild(event), retrieveUrIish(event));
    }

    @Override
    protected URIish retrieveUrIish(PipelineEvent event) {
        try {
            if (event.getProject().getUrl() != null) {
                return new URIish(event.getProject().getUrl());
            }
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "could not parse URL");
        }
        return null;
    }

    @Override
    protected BuildStatusUpdate retrieveBuildStatusUpdate(PipelineEvent event) {
        return buildStatusUpdate()
                .withProjectId(event.getProject().getId())
                .withSha(event.getObjectAttributes().getSha())
                .withRef(event.getObjectAttributes().getRef())
                .build();
    }

    private String retrieveRevisionToBuild(PipelineEvent event) throws NoRevisionToBuildException {
        if (event.getObjectAttributes() != null && event.getObjectAttributes().getSha() != null) {
            return event.getObjectAttributes().getSha();
        } else {
            throw new NoRevisionToBuildException();
        }
    }

    private boolean isLastAlreadyBuild(Job<?, ?> project, PipelineEvent event) {
        ObjectAttributes objectAttributes = event.getObjectAttributes();
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
