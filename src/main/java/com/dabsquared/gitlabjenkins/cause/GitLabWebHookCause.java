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
        putIfNotNull(variables, "gitlabBranch", getBranch());
        putIfNotNull(variables, "gitlabSourceBranch", getSourceBranch());
        putIfNotNull(variables, "gitlabActionType", getActionType().name());
        putIfNotNull(variables, "gitlabUserName", getUserName());
        putIfNotNull(variables, "gitlabUserEmail", getUserEmail());
        putIfNotNull(variables, "gitlabSourceRepoHomepage", getSourceRepoHomepage());
        putIfNotNull(variables, "gitlabSourceRepoName", getSourceRepoName());
        putIfNotNull(variables, "gitlabSourceRepoURL", getSourceRepoUrl());
        putIfNotNull(variables, "gitlabSourceRepoSshUrl", getSourceRepoSshUrl());
        putIfNotNull(variables, "gitlabSourceRepoHttpUrl", getSourceRepoHttpUrl());
        return variables;
    }

    protected void putIfNotNull(Map<String, String> variables, String name, String value) {
        if (value != null) {
            variables.put(name, value);
        }
    }

    protected abstract String getBranch();

    protected abstract String getSourceBranch();

    protected abstract ActionType getActionType();

    protected abstract String getUserName();

    protected abstract String getUserEmail();

    protected abstract String getSourceRepoHomepage();

    protected abstract String getSourceRepoName();

    protected abstract String getSourceRepoUrl();

    protected abstract String getSourceRepoSshUrl();

    protected abstract String getSourceRepoHttpUrl();

    public enum  ActionType {
        PUSH, MERGE
    }
}
