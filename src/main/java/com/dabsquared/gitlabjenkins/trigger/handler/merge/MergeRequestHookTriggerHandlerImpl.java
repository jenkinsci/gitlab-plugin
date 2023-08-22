/* not able to get Oldrev even when the method to obtain it is available in ObjectAttributes?! help */
package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;
import static com.dabsquared.gitlabjenkins.trigger.handler.builder.generated.BuildStatusUpdateBuilder.buildStatusUpdate;
import static com.dabsquared.gitlabjenkins.util.LoggerUtil.toArray;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.gitlab4j.api.Constants.ActionType.valueOf;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.trigger.exception.NoRevisionToBuildException;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import com.dabsquared.gitlabjenkins.trigger.handler.AbstractWebHookTriggerHandler;
import com.dabsquared.gitlabjenkins.util.BuildUtil;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.RevisionParameterAction;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.URIish;
import org.gitlab4j.api.Constants.ActionType;
import org.gitlab4j.api.Constants.MergeRequestState;
import org.gitlab4j.api.webhook.ChangeContainer;
import org.gitlab4j.api.webhook.EventCommit;
import org.gitlab4j.api.webhook.EventLabel;
import org.gitlab4j.api.webhook.MergeRequestChanges;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.MergeRequestEvent.ObjectAttributes;

/**
 * @author Robin Müller
 */
class MergeRequestHookTriggerHandlerImpl extends AbstractWebHookTriggerHandler<MergeRequestEvent>
        implements MergeRequestHookTriggerHandler {

    private static final Logger LOGGER = Logger.getLogger(MergeRequestHookTriggerHandlerImpl.class.getName());

    private final boolean onlyIfNewCommitsPushed;
    private final boolean skipWorkInProgressMergeRequest;
    private final Set<String> labelsThatForcesBuildIfAdded;
    private final Predicate<ObjectAttributes> triggerConfig;
    private final EnumSet<ActionType> skipBuiltYetCheckActions =
            EnumSet.of(ActionType.OPENED, ActionType.APPROVED, ActionType.MERGED);
    private final EnumSet<ActionType> skipAllowedStateForActions = EnumSet.of(ActionType.APPROVED);
    private final boolean cancelPendingBuildsOnUpdate;

    MergeRequestHookTriggerHandlerImpl(
            Collection<MergeRequestState> allowedStates,
            boolean skipWorkInProgressMergeRequest,
            boolean cancelPendingBuildsOnUpdate) {
        this(
                allowedStates,
                EnumSet.noneOf(ActionType.class),
                false,
                skipWorkInProgressMergeRequest,
                cancelPendingBuildsOnUpdate);
    }

    // this retains internal API, however, the plugin code no longer instantiates the handler this way.
    // any code using it should test it on higher level
    @Deprecated
    MergeRequestHookTriggerHandlerImpl(
            Collection<MergeRequestState> allowedStates,
            Collection<ActionType> allowedActions,
            boolean onlyIfNewCommitsPushed,
            boolean skipWorkInProgressMergeRequest,
            boolean cancelPendingBuildsOnUpdate) {
        this(
                new TriggerConfigChain().add(allowedStates, null).add(null, allowedActions),
                onlyIfNewCommitsPushed,
                skipWorkInProgressMergeRequest,
                emptySet(),
                cancelPendingBuildsOnUpdate);
    }

    MergeRequestHookTriggerHandlerImpl(
            Predicate<ObjectAttributes> triggerConfig,
            boolean onlyIfNewCommitsPushed,
            boolean skipWorkInProgressMergeRequest,
            Set<String> labelsThatForcesBuildIfAdded,
            boolean cancelPendingBuildsOnUpdate) {
        this.triggerConfig = triggerConfig;
        this.onlyIfNewCommitsPushed = onlyIfNewCommitsPushed;
        this.skipWorkInProgressMergeRequest = skipWorkInProgressMergeRequest;
        this.labelsThatForcesBuildIfAdded = labelsThatForcesBuildIfAdded;
        this.cancelPendingBuildsOnUpdate = cancelPendingBuildsOnUpdate;
    }

    @Override
    public void handle(
            Job<?, ?> job,
            MergeRequestEvent event,
            boolean ciSkip,
            BranchFilter branchFilter,
            MergeRequestLabelFilter mergeRequestLabelFilter) {
        if (isExecutable(job, event)) {
            List<String> labelsNames = new ArrayList<>();
            if (event.getLabels() != null) {
                for (EventLabel label : event.getLabels()) {
                    labelsNames.add(label.getTitle());
                }
            }

            if (mergeRequestLabelFilter.isMergeRequestAllowed(labelsNames)) {
                super.handle(job, event, ciSkip, branchFilter, mergeRequestLabelFilter);
            }
        }
    }

    protected boolean isNewCommitPushed(MergeRequestEvent event) {
        if (this.onlyIfNewCommitsPushed) {
            if (valueOf(event.getObjectAttributes().getAction().toUpperCase()).equals(ActionType.UPDATED)) {
                if (event.getObjectAttributes().getOldrev() != null) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isExecutable(Job<?, ?> job, MergeRequestEvent event) {
        // conditions to actually trigger a job with GitLab Trigger set
        // 1. config options are OK to react on this MR and MR is not work in progress
        // 2. if MR's labels include label(s) from force label list - build right away
        // 3. if last commit is not yet build...
        // 4. but only if triggerOnlyIfNewCommitsPushed is not set, in other case
        //    only if new commits were pushed to the MR or MR stopped to be work in progress
        ObjectAttributes objectAttributes = event.getObjectAttributes();
        boolean forcedByAddedLabel = isForcedByAddedLabel(event);

        if (isAllowedByConfig(objectAttributes) && isNotSkipWorkInProgressMergeRequest(objectAttributes)) {
            if (forcedByAddedLabel) {
                return true;
            } else {
                if (isLastCommitNotYetBuild(job, event)) {
                    return isNewCommitPushed(event) || isChangedToNotDraft(event);
                }
            }
        }

        return false;
    }

    @Override
    protected boolean isCiSkip(MergeRequestEvent event) {
        return event.getObjectAttributes() != null
                && ((event.getObjectAttributes().getDescription() != null
                                && event.getObjectAttributes().getDescription().contains("[ci-skip]"))
                        || (event.getObjectAttributes().getLastCommit() != null
                                && event.getObjectAttributes().getLastCommit().getMessage() != null
                                && event.getObjectAttributes()
                                        .getLastCommit()
                                        .getMessage()
                                        .contains("[ci-skip]")));
    }

    @Override
    protected void cancelPendingBuildsIfNecessary(Job<?, ?> job, MergeRequestEvent event) {
        if (!this.cancelPendingBuildsOnUpdate) {
            return;
        }
        if (!valueOf(event.getObjectAttributes().getAction().toUpperCase()).equals(ActionType.UPDATED)) {
            return;
        }
        this.pendingBuildsHandler.cancelPendingBuilds(
                job,
                event.getObjectAttributes().getSourceProjectId(),
                event.getObjectAttributes().getSourceBranch());
    }

    @Override
    protected String getSourceBranch(MergeRequestEvent event) {
        return event.getObjectAttributes() == null
                ? null
                : event.getObjectAttributes().getSourceBranch();
    }

    @Override
    protected String getTargetBranch(MergeRequestEvent event) {
        return event.getObjectAttributes() == null
                ? null
                : event.getObjectAttributes().getTargetBranch();
    }

    @Override
    protected String getTriggerType() {
        return "merge request";
    }

    @Override
    protected CauseData retrieveCauseData(MergeRequestEvent event) {
        return causeData()
                .withActionType(CauseData.ActionType.MERGE)
                .withSourceProjectId(event.getObjectAttributes().getSourceProjectId())
                .withTargetProjectId(event.getObjectAttributes().getTargetProjectId())
                .withBranch(event.getObjectAttributes().getSourceBranch())
                .withSourceBranch(event.getObjectAttributes().getSourceBranch())
                .withUserName(
                        event.getObjectAttributes().getLastCommit().getAuthor().getName())
                .withUserEmail(
                        event.getObjectAttributes().getLastCommit().getAuthor().getEmail())
                .withSourceRepoHomepage(event.getObjectAttributes().getSource().getHomepage())
                .withSourceRepoName(event.getObjectAttributes().getSource().getName())
                .withSourceNamespace(event.getObjectAttributes().getSource().getNamespace())
                .withSourceRepoUrl(event.getObjectAttributes().getSource().getUrl())
                .withSourceRepoSshUrl(event.getObjectAttributes().getSource().getSshUrl())
                .withSourceRepoHttpUrl(event.getObjectAttributes().getSource().getHttpUrl())
                .withMergeCommitSha(event.getObjectAttributes().getMergeCommitSha())
                .withMergeRequestTitle(event.getObjectAttributes().getTitle())
                .withMergeRequestDescription(event.getObjectAttributes().getDescription())
                .withMergeRequestId(event.getObjectAttributes().getId())
                .withMergeRequestIid(event.getObjectAttributes().getIid())
                .withMergeRequestState(event.getObjectAttributes().getState())
                .withMergedByUser(
                        event.getUser() == null ? null : event.getUser().getUsername())
                .withMergeRequestAssignee(
                        event.getAssignees() == null
                                ? null
                                : event.getAssignees().get(0).getUsername())
                .withMergeRequestTargetProjectId(event.getObjectAttributes().getTargetProjectId())
                .withTargetBranch(event.getObjectAttributes().getTargetBranch())
                .withTargetRepoName(event.getObjectAttributes().getTarget().getName())
                .withTargetNamespace(event.getObjectAttributes().getTarget().getNamespace())
                .withTargetRepoSshUrl(event.getObjectAttributes().getTarget().getSshUrl())
                .withTargetRepoHttpUrl(event.getObjectAttributes().getTarget().getHttpUrl())
                .withTriggeredByUser(
                        event.getObjectAttributes().getLastCommit().getAuthor().getName())
                .withLastCommit(event.getObjectAttributes().getLastCommit().getId())
                .withTargetProjectUrl(event.getObjectAttributes().getTarget().getWebUrl())
                .build();
    }

    @Override
    protected RevisionParameterAction createRevisionParameter(MergeRequestEvent event, GitSCM gitSCM)
            throws NoRevisionToBuildException {
        return new RevisionParameterAction(retrieveRevisionToBuild(event), retrieveUrIish(event));
    }

    @Override
    protected BuildStatusUpdate retrieveBuildStatusUpdate(MergeRequestEvent event) {
        return buildStatusUpdate()
                .withProjectId(event.getObjectAttributes().getSourceProjectId())
                .withSha(event.getObjectAttributes().getLastCommit().getId())
                .withRef(event.getObjectAttributes().getSourceBranch())
                .build();
    }

    @Override
    protected URIish retrieveUrIish(MergeRequestEvent event) {
        try {
            if (event.getProject() != null) {
                if (event.getProject().getUrl() != null) {
                    return new URIish(event.getProject().getUrl());
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "could not parse URL");
        }
        return null;
    }

    private String retrieveRevisionToBuild(MergeRequestEvent event) throws NoRevisionToBuildException {
        if (event.getObjectAttributes().getMergeCommitSha() != null) {
            return event.getObjectAttributes().getMergeCommitSha();
        } else if (event.getObjectAttributes() != null
                && event.getObjectAttributes().getLastCommit() != null
                && event.getObjectAttributes().getLastCommit().getId() != null) {

            return event.getObjectAttributes().getLastCommit().getId();
        } else {
            throw new NoRevisionToBuildException();
        }
    }

    private boolean isLastCommitNotYetBuild(Job<?, ?> project, MergeRequestEvent event) {
        ObjectAttributes objectAttributes = event.getObjectAttributes();

        if (objectAttributes == null) {
            return true;
        }

        String action = objectAttributes.getAction().toUpperCase();
        if (skipBuiltYetCheckActions.contains(valueOf(action))) {
            LOGGER.log(Level.FINEST, "Skipping LastCommitNotYetBuild check for {0} action", action);
            return true;
        }

        EventCommit lastCommit = objectAttributes.getLastCommit();
        if (lastCommit == null) {
            return true;
        }

        Run<?, ?> mergeBuild = BuildUtil.getBuildBySHA1IncludingMergeBuilds(project, lastCommit.getId());
        if (mergeBuild == null) {
            return true;
        }

        if (!StringUtils.equals(getTargetMergeRequestStateFromBuild(mergeBuild), objectAttributes.getState())) {
            return true;
        }

        if (StringUtils.equals(getTargetBranchFromBuild(mergeBuild), objectAttributes.getTargetBranch())) {
            LOGGER.log(
                    Level.INFO,
                    "Last commit in Merge Request has already been built in build #" + mergeBuild.getNumber());
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

    private boolean isAllowedByConfig(ObjectAttributes objectAttributes) {
        return triggerConfig.test(objectAttributes);
    }

    /**
     * Checks if the MR Title had the 'Draft' keyword removed
     * @param event The event
     * @return True if the 'Draft' keyword was removed from the MR title
     */
    private boolean isChangedToNotDraft(MergeRequestEvent event) {
        ChangeContainer<String> changedTitle = Optional.of(event)
                .map(MergeRequestEvent::getChanges)
                .map(MergeRequestChanges::getTitle)
                .orElse(new ChangeContainer<>());
        String current = changedTitle.getCurrent() != null ? changedTitle.getCurrent() : "";
        String previous = changedTitle.getPrevious() != null ? changedTitle.getPrevious() : "";
        boolean wasDraft = hasDraftIndicator(previous) && !hasDraftIndicator(current);

        // The support of "WIP" is to be removed in GitLab 14.0
        // See here:
        // https://docs.gitlab.com/13.12/ee/user/project/merge_requests/drafts.html#mark-merge-requests-as-drafts
        boolean wasWip = previous.contains("WIP") && !current.contains("WIP");

        return wasDraft || wasWip;
    }

    /**
     * Checks if given text has the appropriate 'Draft' syntax
     *
     * This is intended to be used on a MR Title.
     *
     * The 'Draft' syntax was based off the following documentation:
     * https://docs.gitlab.com/13.12/ee/user/project/merge_requests/drafts.html#mark-merge-requests-as-drafts
     * @param title The title to check
     * @return true if the title starts with the appropriate 'Draft' syntax, else false.
     */
    private static boolean hasDraftIndicator(String title) {
        Pattern draftPattern = Pattern.compile("\\s*(Draft:|\\[Draft\\]|\\(Draft\\)).*");
        return draftPattern.matcher(title).matches();
    }

    private boolean isForcedByAddedLabel(MergeRequestEvent event) {
        if (labelsThatForcesBuildIfAdded.isEmpty()) {
            return false;
        }

        ChangeContainer<List<EventLabel>> changedLabels = Optional.of(event)
                .map(MergeRequestEvent::getChanges)
                .map(MergeRequestChanges::getLabels)
                .orElse(new ChangeContainer<>());
        List<EventLabel> current = changedLabels.getCurrent() != null ? changedLabels.getCurrent() : emptyList();
        List<EventLabel> previous = changedLabels.getPrevious() != null ? changedLabels.getPrevious() : emptyList();

        return current.stream()
                .filter(currentLabel -> previous.stream()
                        .noneMatch(previousLabel -> Objects.equals(currentLabel.getId(), previousLabel.getId())))
                .map(EventLabel::getTitle)
                .anyMatch(labelsThatForcesBuildIfAdded::contains);
    }

    private boolean isNotSkipWorkInProgressMergeRequest(ObjectAttributes objectAttributes) {
        Boolean workInProgress = objectAttributes.getWorkInProgress();
        if (skipWorkInProgressMergeRequest && workInProgress != null && workInProgress) {
            LOGGER.log(
                    Level.INFO,
                    "Skip WIP Merge Request #{0} ({1})",
                    toArray(objectAttributes.getIid(), objectAttributes.getTitle()));
            return false;
        }
        return true;
    }
}
