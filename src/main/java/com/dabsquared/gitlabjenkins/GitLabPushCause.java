package com.dabsquared.gitlabjenkins;

import hudson.triggers.SCMTrigger;

import java.io.File;
import java.io.IOException;

/**
 * UI object that says a build is started by GitHub post-commit hook.
 *
 * @author Daniel Brooks
 */
public class GitLabPushCause extends SCMTrigger.SCMTriggerCause {
    /**
     * The name of the user who pushed to GitHub.
     */
    private String pushedBy;

    public GitLabPushCause(String pusher) {
        this("", pusher);
    }

    public GitLabPushCause(String pollingLog, String pusher) {
        super(pollingLog);
        pushedBy = pusher;
    }

    public GitLabPushCause(File pollingLog, String pusher) throws IOException {
        super(pollingLog);
        pushedBy = pusher;
    }

    @Override
    public String getShortDescription() {
        String pusher = pushedBy != null ? pushedBy : "";
        return "Started by GitLab push by " + pusher;
    }
}
