package com.dabsquared.gitlabjenkins;

import hudson.triggers.SCMTrigger;

import java.io.File;
import java.io.IOException;

/**
 * Created by daniel on 6/8/14.
 */
public class GitLabMergeCause extends SCMTrigger.SCMTriggerCause {

    private final String pushedBy;

    public GitLabMergeCause(String pushedBy) {
        this.pushedBy = pushedBy;
    }

    public GitLabMergeCause(String pushedBy, File logFile) throws IOException {
        super(logFile);
        this.pushedBy = pushedBy;
    }

    public GitLabMergeCause(String pushedBy, String pollingLog) {
        super(pollingLog);
        this.pushedBy = pushedBy;
    }

    @Override
    public String getShortDescription() {
        if (pushedBy == null) {
            return "Started by GitLab Merge Request";
        } else {
            return String.format("Started by GitLab Merge Request by %s", pushedBy);
        }
    }
}
