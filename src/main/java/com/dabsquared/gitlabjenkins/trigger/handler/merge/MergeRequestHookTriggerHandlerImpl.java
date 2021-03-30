package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.*;
import com.dabsquared.gitlabjenkins.trigger.exception.NoRevisionToBuildException;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import com.dabsquared.gitlabjenkins.trigger.handler.AbstractWebHookTriggerHandler;
import com.dabsquared.gitlabjenkins.util.BuildUtil;
import com.dabsquared.gitlabjenkins.trigger.handler.PendingBuildsHandler;
import com.google.common.base.Predicate;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.RevisionParameterAction;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;
import static com.dabsquared.gitlabjenkins.trigger.handler.builder.generated.BuildStatusUpdateBuilder.buildStatusUpdate;
import static com.dabsquared.gitlabjenkins.util.LoggerUtil.toArray;

/**
 * @author Robin Müller
 */
class MergeRequestHookTriggerHandlerImpl extends AbstractWebHookTriggerHandler<MergeRequestHook> implements MergeRequestHookTriggerHandler {

    private static final Logger LOGGER = Logger.getLogger(MergeRequestHookTriggerHandlerImpl.class.getName());

    private final boolean onlyIfNewCommitsPushed;
    private final boolean skipWorkInProgressMergeRequest;
    private final Set<String> labelsThatForcesBuildIfAdded;
    private final Predicate<MergeRequestObjectAttributes> triggerConfig;
    private final EnumSet<Action> skipBuiltYetCheckActions = EnumSet.of(Action.open, Action.approved, Action.merge);
    private final EnumSet<Action> skipAllowedStateForActions = EnumSet.of(Action.approved);
    private final boolean cancelPendingBuildsOnUpdate;

    MergeRequestHookTriggerHandlerImpl(Collection<State> allowedStates, boolean skipWorkInProgressMergeRequest, boolean cancelPendingBuildsOnUpdate) {
        this(allowedStates, EnumSet.noneOf(Action.class), false, skipWorkInProgressMergeRequest, cancelPendingBuildsOnUpdate);
    }

    // this retains internal API, however, the plugin code no longer instantiates the handler this way.
    // any code using it should test it on higher level
    @Deprecated
    MergeRequestHookTriggerHandlerImpl(Collection<State> allowedStates, Collection<Action> allowedActions, boolean onlyIfNewCommitsPushed, boolean skipWorkInProgressMergeRequest, boolean cancelPendingBuildsOnUpdate) {
        this(new TriggerConfigChain().add(allowedStates, null).add(null, allowedActions), onlyIfNewCommitsPushed, skipWorkInProgressMergeRequest, emptySet(), cancelPendingBuildsOnUpdate);
    }

    MergeRequestHookTriggerHandlerImpl(Predicate<MergeRequestObjectAttributes> triggerConfig, boolean onlyIfNewCommitsPushed, boolean skipWorkInProgressMergeRequest, Set<String> labelsThatForcesBuildIfAdded, boolean cancelPendingBuildsOnUpdate) {
        this.triggerConfig = triggerConfig;
        this.onlyIfNewCommitsPushed = onlyIfNewCommitsPushed;
        this.skipWorkInProgressMergeRequest = skipWorkInProgressMergeRequest;
        this.labelsThatForcesBuildIfAdded = labelsThatForcesBuildIfAdded;
        this.cancelPendingBuildsOnUpdate = cancelPendingBuildsOnUpdate;
    }

    @Override
    public void handle(Job<?, ?> job, MergeRequestHook hook, boolean ciSkip, BranchFilter branchFilter, MergeRequestLabelFilter mergeRequestLabelFilter) {
        if (isExecutable(job, hook)) {
            List<String> labelsNames = new ArrayList<>();
            if (hook.getLabels() != null) {
                for (MergeRequestLabel label : hook.getLabels()) {
                    labelsNames.add(label.getTitle());
                }
            }

            if (mergeRequestLabelFilter.isMergeRequestAllowed(labelsNames)) {
                super.handle(job, hook, ciSkip, branchFilter, mergeRequestLabelFilter);
            }
        }
    }

    protected boolean isNewCommitPushed(MergeRequestHook hook) {
        if (this.onlyIfNewCommitsPushed) {
            if (hook.getObjectAttributes().getAction().equals(Action.update)) {
                return hook.getObjectAttributes().getOldrev() != null;
            }
        }
        return true;
    }

    private boolean isExecutable(Job<?, ?> job, MergeRequestHook hook) {
        // conditions to actually trigger a job with GitLab Trigger set
        // 1. config options are OK to react on this MR and MR is not work in progress
        // 2. if MR's labels include label(s) from force label list - build right away
        // 3. if last commit is not yet build...
        // 4. but only if triggerOnlyIfNewCommitsPushed is not set, in other case
        //    only if new commits were pushed to the MR or MR stopped to be work in progress
        MergeRequestObjectAttributes objectAttributes = hook.getObjectAttributes();
        boolean forcedByAddedLabel = isForcedByAddedLabel(hook);

        if (isAllowedByConfig(objectAttributes) && isNotSkipWorkInProgressMergeRequest(objectAttributes)) {
            if (forcedByAddedLabel) {
                return true;
            } else {
                if (isLastCommitNotYetBuild(job, hook)) {
                    return isNewCommitPushed(hook) || isBecameNoWip(hook);
                }
            }
        }

        return false;
    }

    @Override
    protected boolean isCiSkip(MergeRequestHook hook) {
        return hook.getObjectAttributes() != null
            && (
                (hook.getObjectAttributes().getDescription() != null
                    && hook.getObjectAttributes().getDescription().contains("[ci-skip]")
                ) ||
                (
                    hook.getObjectAttributes().getLastCommit() != null &&
                        hook.getObjectAttributes().getLastCommit().getMessage() != null &&
                        hook.getObjectAttributes().getLastCommit().getMessage().contains("[ci-skip]")
                )
        );
    }

    @Override
    protected void cancelPendingBuildsIfNecessary(Job<?, ?> job, MergeRequestHook hook) {
        if (!this.cancelPendingBuildsOnUpdate) {
            return;
        }
        if (!hook.getObjectAttributes().getAction().equals(Action.update)) {
            return;
        }
        this.pendingBuildsHandler.cancelPendingBuilds(job, hook.getObjectAttributes().getSourceProjectId(), hook.getObjectAttributes().getSourceBranch());
    }

    @Override
    protected String getSourceBranch(MergeRequestHook hook) {
        return hook.getObjectAttributes() == null ? null : hook.getObjectAttributes().getSourceBranch();
    }

    @Override
    protected String getTargetBranch(MergeRequestHook hook) {
        return hook.getObjectAttributes() == null ? null : hook.getObjectAttributes().getTargetBranch();
    }

    @Override
    protected String getTriggerType() {
        return "merge request";
    }

    @Override
    protected CauseData retrieveCauseData(MergeRequestHook hook) {
        return causeData()
                .withActionType(CauseData.ActionType.MERGE)
                .withSourceProjectId(hook.getObjectAttributes().getSourceProjectId())
                .withTargetProjectId(hook.getObjectAttributes().getTargetProjectId())
                .withBranch(hook.getObjectAttributes().getSourceBranch())
                .withSourceBranch(hook.getObjectAttributes().getSourceBranch())
                .withUserName(hook.getObjectAttributes().getLastCommit().getAuthor().getName())
                .withUserEmail(hook.getObjectAttributes().getLastCommit().getAuthor().getEmail())
                .withSourceRepoHomepage(hook.getObjectAttributes().getSource().getHomepage())
                .withSourceRepoName(hook.getObjectAttributes().getSource().getName())
                .withSourceNamespace(hook.getObjectAttributes().getSource().getNamespace())
                .withSourceRepoUrl(hook.getObjectAttributes().getSource().getUrl())
                .withSourceRepoSshUrl(hook.getObjectAttributes().getSource().getSshUrl())
                .withSourceRepoHttpUrl(hook.getObjectAttributes().getSource().getHttpUrl())
                .withMergeRequestTitle(hook.getObjectAttributes().getTitle())
                .withMergeRequestDescription(hook.getObjectAttributes().getDescription())
                .withMergeRequestId(hook.getObjectAttributes().getId())
                .withMergeRequestIid(hook.getObjectAttributes().getIid())
                .withMergeRequestState(hook.getObjectAttributes().getState().toString())
                .withMergedByUser(hook.getUser() == null ? null : hook.getUser().getUsername())
                .withMergeRequestAssignee(hook.getAssignee() == null ? null : hook.getAssignee().getUsername())
                .withMergeRequestTargetProjectId(hook.getObjectAttributes().getTargetProjectId())
                .withTargetBranch(hook.getObjectAttributes().getTargetBranch())
                .withTargetRepoName(hook.getObjectAttributes().getTarget().getName())
                .withTargetNamespace(hook.getObjectAttributes().getTarget().getNamespace())
                .withTargetRepoSshUrl(hook.getObjectAttributes().getTarget().getSshUrl())
                .withTargetRepoHttpUrl(hook.getObjectAttributes().getTarget().getHttpUrl())
                .withTriggeredByUser(hook.getObjectAttributes().getLastCommit().getAuthor().getName())
                .withLastCommit(hook.getObjectAttributes().getLastCommit().getId())
                .withTargetProjectUrl(hook.getObjectAttributes().getTarget().getWebUrl())
                .build();
    }

    @Override
    protected RevisionParameterAction createRevisionParameter(MergeRequestHook hook, GitSCM gitSCM) throws NoRevisionToBuildException {
        return new RevisionParameterAction(retrieveRevisionToBuild(hook), retrieveUrIish(hook));
    }

    @Override
    protected BuildStatusUpdate retrieveBuildStatusUpdate(MergeRequestHook hook) {
        return buildStatusUpdate()
            .withProjectId(hook.getObjectAttributes().getSourceProjectId())
            .withSha(hook.getObjectAttributes().getLastCommit().getId())
            .withRef(hook.getObjectAttributes().getSourceBranch())
            .build();
    }

    private String retrieveRevisionToBuild(MergeRequestHook hook) throws NoRevisionToBuildException {
        if (hook.getObjectAttributes() != null
                && hook.getObjectAttributes().getLastCommit() != null
                && hook.getObjectAttributes().getLastCommit().getId() != null) {

            return hook.getObjectAttributes().getLastCommit().getId();
        } else {
            throw new NoRevisionToBuildException();
        }
    }

    private boolean isLastCommitNotYetBuild(Job<?, ?> project, MergeRequestHook hook) {
        MergeRequestObjectAttributes objectAttributes = hook.getObjectAttributes();

        if (objectAttributes == null) {
            return true;
        }

        Action action = objectAttributes.getAction();
        if (skipBuiltYetCheckActions.contains(action)) {
            LOGGER.log(Level.FINEST, "Skipping LastCommitNotYetBuild check for {0} action", action);
            return true;
        }

        Commit lastCommit = objectAttributes.getLastCommit();
        if (lastCommit == null) {
            return true;
        }

        Run<?, ?> mergeBuild = BuildUtil.getBuildBySHA1IncludingMergeBuilds(project, lastCommit.getId());
        if (mergeBuild == null) {
            return true;
        }

        if (!StringUtils.equals(getTargetMergeRequestStateFromBuild(mergeBuild), objectAttributes.getState().name())) {
            return true;
        }

        if (StringUtils.equals(getTargetBranchFromBuild(mergeBuild), objectAttributes.getTargetBranch())) {
            LOGGER.log(Level.INFO, "Last commit in Merge Request has already been built in build #" + mergeBuild.getNumber());
            return false;
        }

        return true;
    }

    private String getTargetBranchFromBuild(Run<?, ?> mergeBuild) {
        GitLabWebHookCause cause = mergeBuild.getCause(GitLabWebHookCause.class);
        return cause == null ? null : cause.getData().getTargetBranch();
    }

    private String getTargetMergeRequestStateFromBuild(Run<?, ?> mergeBuild) {
        GitLabWebHookCause cause = mergeBuild.getCause(GitLabWebHookCause.class);
        return cause == null ? null : cause.getData().getMergeRequestState();
    }

	private boolean isAllowedByConfig(MergeRequestObjectAttributes objectAttributes) {
		return triggerConfig.apply(objectAttributes);
    }
    private boolean isBecameNoWip(MergeRequestHook hook) {
        MergeRequestChangedTitle changedTitle = Optional.of(hook).map(MergeRequestHook::getChanges).map(MergeRequestChanges::getTitle).orElse(new MergeRequestChangedTitle());
        String current = changedTitle.getCurrent() != null ? changedTitle.getCurrent() : "";
        String previous = changedTitle.getPrevious() != null ? changedTitle.getPrevious() : "";

        return previous.contains("WIP") && !current.contains("WIP");
    }

    private boolean isForcedByAddedLabel(MergeRequestHook hook) {
        if (labelsThatForcesBuildIfAdded.isEmpty()) {
            return false;
        }

        MergeRequestChangedLabels changedLabels = Optional.of(hook).map(MergeRequestHook::getChanges).map(MergeRequestChanges::getLabels).orElse(new MergeRequestChangedLabels());
        List<MergeRequestLabel> current = changedLabels.getCurrent() != null ? changedLabels.getCurrent() : emptyList();
        List<MergeRequestLabel> previous = changedLabels.getPrevious() != null ? changedLabels.getPrevious() : emptyList();

        return current.stream()
                .filter(currentLabel -> !previous.stream().anyMatch(previousLabel -> Objects.equals(currentLabel.getId(), previousLabel.getId())))
                .map(label -> label.getTitle())
                .anyMatch(label -> labelsThatForcesBuildIfAdded.contains(label));
    }

    private boolean isNotSkipWorkInProgressMergeRequest(MergeRequestObjectAttributes objectAttributes) {
        Boolean workInProgress = objectAttributes.getWorkInProgress();
        if (skipWorkInProgressMergeRequest && workInProgress != null && workInProgress) {
            LOGGER.log(Level.INFO, "Skip WIP Merge Request #{0} ({1})", toArray(objectAttributes.getIid(), objectAttributes.getTitle()));
            return false;
        }
        return true;
    }
}
