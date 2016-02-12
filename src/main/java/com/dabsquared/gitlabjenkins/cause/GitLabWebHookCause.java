package com.dabsquared.gitlabjenkins.cause;

import com.dabsquared.gitlabjenkins.GitLabRequest;
import hudson.triggers.SCMTrigger;

import java.io.File;
import java.io.IOException;

/**
 * @author Robin Müller
 */
public class GitLabWebHookCause<T extends GitLabRequest> extends SCMTrigger.SCMTriggerCause {

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
}
