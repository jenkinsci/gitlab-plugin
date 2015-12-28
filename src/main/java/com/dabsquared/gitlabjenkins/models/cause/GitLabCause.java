package com.dabsquared.gitlabjenkins.models.cause;

import com.dabsquared.gitlabjenkins.models.request.GitLabRequest;
import hudson.triggers.SCMTrigger;

import java.io.File;
import java.io.IOException;

public abstract class GitLabCause<T extends GitLabRequest> extends SCMTrigger.SCMTriggerCause {

    protected T request;

    public GitLabCause(T request, File logFile) throws IOException {
        super(logFile);
        this.request = request;
    }

    public GitLabCause(T request, String pollingLog) {
        super(pollingLog);
        this.request = request;
    }

    public GitLabCause(T request) {
        super();
        this.request = request;
    }

    public T getRequest() {
        return request;
    }


}
