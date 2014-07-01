package com.dabsquared.gitlabjenkins;

import hudson.triggers.SCMTrigger;

import java.io.File;
import java.io.IOException;

/**
 * Created by daniel on 6/8/14.
 */
public class GitLabPushCause extends SCMTrigger.SCMTriggerCause {

    private final String pushedBy;

    public GitLabPushCause(String pushedBy) {
        this.pushedBy = pushedBy;
    }

    public GitLabPushCause(String pushedBy, File logFile) throws IOException {
        super(logFile);
        this.pushedBy = pushedBy;
    }

    public GitLabPushCause(String pushedBy, String pollingLog) {
        super(pollingLog);
        this.pushedBy = pushedBy;
    }

    @Override
    public String getShortDescription() {
        if (pushedBy == null) {
            return "Started by GitLab push";
        } else {
            return String.format("Started by GitLab push by %s", pushedBy);
        }
    }
}
