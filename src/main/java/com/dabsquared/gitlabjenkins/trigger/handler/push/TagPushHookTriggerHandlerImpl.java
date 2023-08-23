package com.dabsquared.gitlabjenkins.trigger.handler.push;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;
import static com.dabsquared.gitlabjenkins.trigger.handler.builder.generated.BuildStatusUpdateBuilder.buildStatusUpdate;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.trigger.exception.NoRevisionToBuildException;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import com.dabsquared.gitlabjenkins.trigger.handler.AbstractWebHookTriggerHandler;
import hudson.model.Job;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.RevisionParameterAction;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.StringUtils;
import org.gitlab4j.api.webhook.EventCommit;
import org.gitlab4j.api.webhook.TagPushEvent;

class TagPushHookTriggerHandlerImpl extends AbstractWebHookTriggerHandler<TagPushEvent>
        implements TagPushHookTriggerHandler {

    private static final String NO_COMMIT = "0000000000000000000000000000000000000000";
    private boolean triggerToBranchDeleteRequest = false;

    private static final Logger LOGGER = Logger.getLogger(PushHookTriggerHandlerImpl.class.getName());

    public TagPushHookTriggerHandlerImpl(boolean triggerToBranchDeleteRequest) {
        this.triggerToBranchDeleteRequest = triggerToBranchDeleteRequest;
    }

    @Override
    public void handle(
            Job<?, ?> job,
            TagPushEvent event,
            boolean ciSkip,
            BranchFilter branchFilter,
            MergeRequestLabelFilter mergeRequestLabelFilter) {
        if (isNoRemoveBranchPush(event) || this.triggerToBranchDeleteRequest) {
            super.handle(job, event, ciSkip, branchFilter, mergeRequestLabelFilter);
        }
    }

    @Override
    protected boolean isCiSkip(TagPushEvent event) {
        List<EventCommit> commits = event.getCommits();
        return commits != null
                && !commits.isEmpty()
                && commits.get(commits.size() - 1).getMessage() != null
                && commits.get(commits.size() - 1).getMessage().contains("[ci-skip]");
    }

    @Override
    protected CauseData retrieveCauseData(TagPushEvent event) {
        try {
            CauseData.ActionType actionType = CauseData.ActionType.PUSH;
            return causeData()
                    .withActionType(actionType)
                    .withSourceProjectId(event.getProjectId())
                    .withTargetProjectId(event.getProjectId())
                    .withBranch(getTargetBranch(event))
                    .withSourceBranch(getTargetBranch(event))
                    .withUserName(event.getUserName())
                    .withUserUsername(event.getUserUsername())
                    .withUserEmail(event.getUserEmail())
                    .withSourceRepoHomepage(event.getRepository().getHomepage())
                    .withSourceRepoName(event.getRepository().getName())
                    .withSourceNamespace(event.getProject().getNamespace())
                    .withSourceRepoUrl(event.getRepository().getUrl())
                    .withSourceRepoSshUrl(event.getRepository().getGit_ssh_url())
                    .withSourceRepoHttpUrl(event.getRepository().getGit_http_url())
                    .withMergeCommitSha(null)
                    .withMergeRequestTitle("")
                    .withMergeRequestDescription("")
                    .withMergeRequestId(null)
                    .withMergeRequestIid(null)
                    .withMergeRequestState(null)
                    .withMergedByUser("")
                    .withMergeRequestAssignee("")
                    .withMergeRequestTargetProjectId(null)
                    .withTargetBranch(getTargetBranch(event))
                    .withTargetRepoName("")
                    .withTargetNamespace("")
                    .withTargetRepoSshUrl("")
                    .withTargetRepoHttpUrl("")
                    .withTriggeredByUser(retrievePushedBy(event))
                    .withBefore(event.getBefore())
                    .withAfter(event.getAfter())
                    .withLastCommit(event.getAfter())
                    .withTargetProjectUrl(event.getProject().getWebUrl())
                    .build();
        } catch (NullPointerException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            return null;
        }
    }

    @Override
    protected String getSourceBranch(TagPushEvent event) {
        return event.getRef() == null ? null : event.getRef().replaceFirst("^refs/heads/", "");
    }

    @Override
    protected String getTargetBranch(TagPushEvent event) {
        return event.getRef() == null ? null : event.getRef().replaceFirst("^refs/heads/", "");
    }

    @Override
    protected String getTriggerType() {
        return "push";
    }

    @Override
    protected RevisionParameterAction createRevisionParameter(TagPushEvent event, GitSCM gitSCM)
            throws NoRevisionToBuildException {
        return new RevisionParameterAction(retrieveRevisionToBuild(event, gitSCM), retrieveUrIish(event));
    }

    @Override
    protected BuildStatusUpdate retrieveBuildStatusUpdate(TagPushEvent event) {
        return buildStatusUpdate()
                .withProjectId(event.getProjectId())
                .withSha(event.getAfter())
                .withRef(getTargetBranch(event))
                .build();
    }

    @Override
    protected URIish retrieveUrIish(TagPushEvent event) {
        try {
            if (event.getProject().getUrl() != null) {
                return new URIish(event.getProject().getUrl());
            }
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "could not parse URL");
        }
        return null;
    }

    private String retrievePushedBy(final TagPushEvent event) {
        final String userName = event.getUserName();
        if (!StringUtils.isEmptyOrNull(userName)) {
            return userName;
        }

        final String userUsername = event.getUserUsername();
        if (!StringUtils.isEmptyOrNull(userUsername)) {
            return userUsername;
        }

        final List<EventCommit> commits = event.getCommits();
        if (commits != null && !commits.isEmpty()) {
            return commits.get(commits.size() - 1).getAuthor().getName();
        }

        return null;
    }

    private String retrieveRevisionToBuild(TagPushEvent event, GitSCM gitSCM) throws NoRevisionToBuildException {
        if (inNoBranchDelete(event)) {
            if (gitSCM != null && gitSCM.getRepositories().size() == 1) {
                String repositoryName = gitSCM.getRepositories().get(0).getName();
                return event.getRef().replaceFirst("^refs/heads", "remotes/" + repositoryName);
            } else {
                return event.getAfter();
            }
        } else {
            throw new NoRevisionToBuildException();
        }
    }

    private boolean inNoBranchDelete(TagPushEvent event) {
        return event.getAfter() != null && !event.getAfter().equals(NO_COMMIT);
    }

    private boolean isNoRemoveBranchPush(TagPushEvent event) {
        return event.getAfter() != null && !event.getAfter().equals(NO_COMMIT);
    }
}
