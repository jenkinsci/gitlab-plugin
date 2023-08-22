package com.dabsquared.gitlabjenkins.trigger.handler.note;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.URIish;
import org.gitlab4j.api.webhook.NoteEvent;

/**
 * @author Nikolay Ustinov
 */
class NoteHookTriggerHandlerImpl extends AbstractWebHookTriggerHandler<NoteEvent> implements NoteHookTriggerHandler {

    private static final Logger LOGGER = Logger.getLogger(NoteHookTriggerHandlerImpl.class.getName());

    private final String noteRegex;

    NoteHookTriggerHandlerImpl(String noteRegex) {
        this.noteRegex = noteRegex;
    }

    @Override
    public void handle(
            Job<?, ?> job,
            NoteEvent event,
            boolean ciSkip,
            BranchFilter branchFilter,
            MergeRequestLabelFilter mergeRequestLabelFilter) {
        if (isValidTriggerPhrase(event.getObjectAttributes().getNote())) {
            super.handle(job, event, ciSkip, branchFilter, mergeRequestLabelFilter);
        }
    }

    @Override
    protected boolean isCiSkip(NoteEvent event) {
        return event.getMergeRequest() != null
                && event.getMergeRequest().getDescription() != null
                && event.getMergeRequest().getDescription().contains("[ci-skip]");
    }

    @Override
    protected String getSourceBranch(NoteEvent event) {
        return event.getMergeRequest() == null ? null : event.getMergeRequest().getSourceBranch();
    }

    @Override
    protected String getTargetBranch(NoteEvent hook) {
        return hook.getMergeRequest() == null ? null : hook.getMergeRequest().getTargetBranch();
    }

    @Override
    protected String getTriggerType() {
        return "note";
    }

    @Override
    protected CauseData retrieveCauseData(NoteEvent event) {
        return causeData()
                .withActionType(CauseData.ActionType.NOTE)
                .withSourceProjectId(event.getMergeRequest().getSourceProjectId())
                .withTargetProjectId(event.getMergeRequest().getTargetProjectId())
                .withBranch(event.getMergeRequest().getSourceBranch())
                .withSourceBranch(event.getMergeRequest().getSourceBranch())
                .withUserName(
                        event.getMergeRequest().getLastCommit().getAuthor().getName())
                .withUserEmail(
                        event.getMergeRequest().getLastCommit().getAuthor().getEmail())
                .withSourceRepoHomepage(event.getMergeRequest().getSource().getHomepage())
                .withSourceRepoName(event.getMergeRequest().getSource().getName())
                .withSourceNamespace(event.getMergeRequest().getSource().getNamespace())
                .withSourceRepoUrl(event.getMergeRequest().getSource().getUrl())
                .withSourceRepoSshUrl(event.getMergeRequest().getSource().getSshUrl())
                .withSourceRepoHttpUrl(event.getMergeRequest().getSource().getHttpUrl())
                .withMergeRequestTitle(event.getMergeRequest().getTitle())
                .withMergeRequestDescription(event.getMergeRequest().getDescription())
                .withMergeRequestId(event.getMergeRequest().getId())
                .withMergeRequestIid(event.getMergeRequest().getIid())
                .withMergeRequestTargetProjectId(event.getMergeRequest().getTargetProjectId())
                .withTargetBranch(event.getMergeRequest().getTargetBranch())
                .withTargetRepoName(event.getMergeRequest().getTarget().getName())
                .withTargetNamespace(event.getMergeRequest().getTarget().getNamespace())
                .withTargetRepoSshUrl(event.getMergeRequest().getTarget().getSshUrl())
                .withTargetRepoHttpUrl(event.getMergeRequest().getTarget().getHttpUrl())
                .withTriggeredByUser(
                        event.getMergeRequest().getLastCommit().getAuthor().getName())
                .withLastCommit(event.getMergeRequest().getLastCommit().getId())
                .withTargetProjectUrl(event.getMergeRequest().getTarget().getWebUrl())
                .withTriggerPhrase(event.getObjectAttributes().getNote())
                .withCommentAuthor(
                        event.getUser() == null ? null : event.getUser().getUsername())
                .build();
    }

    @Override
    protected RevisionParameterAction createRevisionParameter(NoteEvent event, GitSCM gitSCM)
            throws NoRevisionToBuildException {
        return new RevisionParameterAction(retrieveRevisionToBuild(event), retrieveUrIish(event));
    }

    @Override
    protected BuildStatusUpdate retrieveBuildStatusUpdate(NoteEvent event) {
        return buildStatusUpdate()
                .withProjectId(event.getMergeRequest().getSourceProjectId())
                .withSha(event.getMergeRequest().getLastCommit().getId())
                .withRef(event.getMergeRequest().getSourceBranch())
                .build();
    }

    @Override
    protected URIish retrieveUrIish(NoteEvent event) {
        try {
            if (event.getProject().getUrl() != null) {
                return new URIish(event.getProject().getUrl());
            }
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "could not parse URL");
        }
        return null;
    }

    private String retrieveRevisionToBuild(NoteEvent event) throws NoRevisionToBuildException {
        if (event.getMergeRequest() != null
                && event.getMergeRequest().getLastCommit() != null
                && event.getMergeRequest().getLastCommit().getId() != null) {

            return event.getMergeRequest().getLastCommit().getId();
        } else {
            throw new NoRevisionToBuildException();
        }
    }

    private boolean isValidTriggerPhrase(String note) {
        if (StringUtils.isEmpty(this.noteRegex)) {
            return false;
        }
        final Pattern pattern = Pattern.compile(this.noteRegex);
        return pattern.matcher(note).matches();
    }
}
