package com.dabsquared.gitlabjenkins.cause;

import com.dabsquared.gitlabjenkins.model.WebHook;
import hudson.triggers.SCMTrigger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Robin Müller
 */
public abstract class GitLabWebHookCause<T extends WebHook> extends SCMTrigger.SCMTriggerCause {

    private final T request;

    public GitLabWebHookCause(T request, String pollingLog) {
        super(pollingLog);
        this.request = request;
    }

    public GitLabWebHookCause(T request, File logFile) throws IOException {
        super(logFile);
        this.request = request;
    }

    public T getRequest() {
        return request;
    }

    public Map<String, String> getBuildVariables() {
        HashMap<String, String> variables = new HashMap<String, String>();
        putWithNullCheck(variables, "gitlabBranch", getBranch());
        putWithNullCheck(variables, "gitlabSourceBranch", getSourceBranch());
        putWithNullCheck(variables, "gitlabActionType", getActionType().name());
        putWithNullCheck(variables, "gitlabUserName", getUserName());
        putWithNullCheck(variables, "gitlabUserEmail", getUserEmail());
        putWithNullCheck(variables, "gitlabSourceRepoHomepage", getSourceRepoHomepage());
        putWithNullCheck(variables, "gitlabSourceRepoName", getSourceRepoName());
        putWithNullCheck(variables, "gitlabSourceRepoURL", getSourceRepoUrl());
        putWithNullCheck(variables, "gitlabSourceRepoSshUrl", getSourceRepoSshUrl());
        putWithNullCheck(variables, "gitlabSourceRepoHttpUrl", getSourceRepoHttpUrl());
        putWithNullCheck(variables, "gitlabMergeRequestTitle", getMergeRequestTitle());
        putWithNullCheck(variables, "gitlabMergeRequestDescription", getMergeRequestDescription());
        putWithNullCheck(variables, "gitlabMergeRequestId", getMergeRequestId());
        putWithNullCheck(variables, "gitlabTargetBranch", getTargetBranch());
        putWithNullCheck(variables, "gitlabTargetRepoName", getTargetRepoName());
        putWithNullCheck(variables, "gitlabTargetRepoSshUrl", getTargetRepoSshUrl());
        putWithNullCheck(variables, "gitlabTargetRepoHttpUrl", getTargetRepoHttpUrl());
        return variables;
    }

    protected void putWithNullCheck(Map<String, String> variables, String name, String value) {
        variables.put(name, value == null ? "" : value);
    }

    public abstract String getBranch();

    public abstract String getSourceBranch();

    public abstract ActionType getActionType();

    public abstract String getUserName();

    public abstract String getUserEmail();

    public abstract String getSourceRepoHomepage();

    public abstract String getSourceRepoName();

    public abstract String getSourceRepoUrl();

    public abstract String getSourceRepoSshUrl();

    public abstract String getSourceRepoHttpUrl();

    public String getMergeRequestTitle() {
        return null;
    }

    public String getMergeRequestDescription() {
        return null;
    }

    public String getMergeRequestId() {
        return null;
    }

    public String getTargetBranch() {
        return null;
    }

    public String getTargetRepoName() {
        return null;
    }

    public String getTargetRepoSshUrl() {
        return null;
    }

    public String getTargetRepoHttpUrl() {
        return null;
    }

    public enum  ActionType {
        PUSH, MERGE
    }
}
