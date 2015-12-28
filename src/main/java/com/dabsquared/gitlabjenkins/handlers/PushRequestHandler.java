package com.dabsquared.gitlabjenkins.handlers;

import com.dabsquared.gitlabjenkins.models.User;
import com.dabsquared.gitlabjenkins.models.cause.GitLabPushCause;
import com.dabsquared.gitlabjenkins.models.request.GitLabPushRequest;
import com.dabsquared.gitlabjenkins.types.GitlabActionType;
import com.dabsquared.gitlabjenkins.types.Parameters;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import hudson.plugins.git.RevisionParameterAction;
import hudson.triggers.SCMTrigger;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PushRequestHandler extends GitlabRequestHandler<GitLabPushRequest> {
    private static final Logger LOGGER = Logger.getLogger(PushRequestHandler.class.getName());

    public PushRequestHandler(Job job, GitLabPushRequest req, boolean addCiMessage) {
        super(job, req, addCiMessage);
    }

    @Override
    protected SCMTrigger.SCMTriggerCause createCause() {
        GitLabPushCause cause;
        try {
            cause = new GitLabPushCause(request, getLogFile());
        } catch (IOException ex) {
            cause = new GitLabPushCause(request);
        }
        return cause;
    }

    @Override
    protected Action[] createActions() {
        String branch = getSourceBranch();

        LOGGER.log(Level.INFO, "GitLab Push Request from branch {0}.", branch);

        Map<String, ParameterValue> values = getDefaultParameters(request);

        values.put(Parameters.GITLAB_TARGET_BRANCH, new StringParameterValue(Parameters.GITLAB_TARGET_BRANCH, branch));
        values.put(GITLAB_BRANCH, new StringParameterValue(GITLAB_BRANCH, branch));
        values.put(GITLAB_ACTION_TYPE, new StringParameterValue(GITLAB_ACTION_TYPE, GitlabActionType.PUSH.name().toLowerCase()));

        User author = request.getCommits().get(0).getAuthor();
        if (author.getName() != null) {
            values.put(GITLAB_USER_NAME, new StringParameterValue(GITLAB_USER_NAME, author.getName()));
        }
        if (author.getEmail() != null) {
            values.put(GITLAB_USER_EMAIL, new StringParameterValue(GITLAB_USER_EMAIL, author.getEmail()));
        }
        values.put(GITLAB_MERGE_REQUEST_TITLE, new StringParameterValue(GITLAB_MERGE_REQUEST_TITLE, ""));
        values.put(GITLAB_MERGE_REQUEST_ID, new StringParameterValue(GITLAB_MERGE_REQUEST_ID, ""));
        values.put(GITLAB_MERGE_REQUEST_ASSIGNEE, new StringParameterValue(GITLAB_MERGE_REQUEST_ASSIGNEE, ""));


        RevisionParameterAction revision = createPushRequestRevisionParameter(job, request);
        if (revision == null) {
            return new Action[0];
        }

        return makeParameterActions(values, revision);
    }

    public static RevisionParameterAction createPushRequestRevisionParameter(Job<?, ?> job, GitLabPushRequest request) {
        RevisionParameterAction revision;

        if (request.getLastCommit() != null) {
            revision = new RevisionParameterAction(request.getLastCommit().getId());
        } else {
            if (request.getCheckout_sha() != null) {
                if (request.getCheckout_sha().contains(NO_COMMIT_HASH)) {
                    // no commit and no checkout sha, a Tag was deleted, so no build need to be triggered
                    LOGGER.log(Level.INFO, "GitLab Push {0} has been deleted, skip build .", request.getRef());
                    return null;
                }
                revision = new RevisionParameterAction(request.getCheckout_sha());
            } else if (request.getBefore() != null
                    && request.getBefore().contains(NO_COMMIT_HASH)) {
                // new branches
                revision = new RevisionParameterAction(request.getAfter());
            } else {
                LOGGER.log(Level.WARNING,
                        "unknown handled situation, dont know what revision to build for req {0} for job {1}",
                        new Object[]{request, (job != null ? job.getFullName() : null)});
                return null;
            }
        }
        return revision;
    }

    @Override
    public String getSourceBranch() {
        return request.getRef().replaceAll("refs/heads/", "");
    }
}
