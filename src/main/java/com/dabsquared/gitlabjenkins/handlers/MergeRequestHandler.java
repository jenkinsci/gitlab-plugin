package com.dabsquared.gitlabjenkins.handlers;

import com.dabsquared.gitlabjenkins.models.User;
import com.dabsquared.gitlabjenkins.models.cause.GitLabMergeCause;
import com.dabsquared.gitlabjenkins.models.request.GitLabMergeRequest;
import com.dabsquared.gitlabjenkins.types.GitlabActionType;
import com.dabsquared.gitlabjenkins.types.Parameters;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import hudson.triggers.SCMTrigger;

import java.io.IOException;
import java.util.Map;

public class MergeRequestHandler extends GitlabRequestHandler<GitLabMergeRequest> {
    public MergeRequestHandler(Job job, GitLabMergeRequest req, boolean addCiMessage) {
        super(job, req, addCiMessage);
    }

    @Override
    protected SCMTrigger.SCMTriggerCause createCause() {
        GitLabMergeCause cause;
        try {
            cause = new GitLabMergeCause(request, getLogFile());
        } catch (IOException ex) {
            cause = new GitLabMergeCause(request);
        }
        return cause;
    }

    @Override
    protected Action[] createActions() {
        Map<String, ParameterValue> values = getDefaultParameters(request);
        values.put(Parameters.GITLAB_TARGET_BRANCH, new StringParameterValue(Parameters.GITLAB_TARGET_BRANCH, request.getObjectAttribute().getTargetBranch()));
        values.put(GITLAB_ACTION_TYPE, new StringParameterValue(GITLAB_ACTION_TYPE, GitlabActionType.MERGE.name().toLowerCase()));
        User author = request.getObjectAttribute().getAuthor();
        if (author != null) {
            if (author.getName() != null) {
                values.put(GITLAB_USER_NAME, new StringParameterValue(GITLAB_USER_NAME, author.getName()));
            }
            if (author.getEmail() != null) {
                values.put(GITLAB_USER_EMAIL, new StringParameterValue(GITLAB_USER_EMAIL, author.getEmail()));
            }
        }
        values.put(GITLAB_MERGE_REQUEST_TITLE, new StringParameterValue(GITLAB_MERGE_REQUEST_TITLE, request.getObjectAttribute().getTitle()));
        values.put(GITLAB_MERGE_REQUEST_ID, new StringParameterValue(GITLAB_MERGE_REQUEST_ID, request.getObjectAttribute().getIid().toString()));
        if (request.getObjectAttribute().getAssignee() != null) {
            values.put(GITLAB_MERGE_REQUEST_ASSIGNEE, new StringParameterValue(GITLAB_MERGE_REQUEST_ASSIGNEE, request.getObjectAttribute().getAssignee().getName()));
        }

        return makeParameterActions(values);
    }

    @Override
    protected String getSourceBranch() {
        return request.getObjectAttribute().getSourceBranch();
    }
}
