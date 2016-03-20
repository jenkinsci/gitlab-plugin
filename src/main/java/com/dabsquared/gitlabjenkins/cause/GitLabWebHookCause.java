package com.dabsquared.gitlabjenkins.cause;

import hudson.triggers.SCMTrigger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Robin Müller
 */
public class GitLabWebHookCause extends SCMTrigger.SCMTriggerCause {

    private final CauseData data;

    public GitLabWebHookCause(CauseData data) {
        super("");
        this.data = checkNotNull(data, "data must not be null");
    }

    public CauseData getData() {
        return data;
    }

    @Override
    public String getShortDescription() {
        return data.getShortDescription();
    }
}
