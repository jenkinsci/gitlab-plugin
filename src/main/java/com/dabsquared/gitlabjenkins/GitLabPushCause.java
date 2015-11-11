package com.dabsquared.gitlabjenkins;

import hudson.triggers.SCMTrigger;

import java.io.File;
import java.io.IOException;

/**
 * Created by daniel on 6/8/14.
 */
public class GitLabPushCause extends SCMTrigger.SCMTriggerCause {

    private final GitLabPushRequest pushRequest;

    public GitLabPushCause(GitLabPushRequest pushRequest) {
        this.pushRequest=pushRequest;
    }

    public GitLabPushCause(GitLabPushRequest pushRequest, File logFile) throws IOException{
        super(logFile);
        this.pushRequest=pushRequest;
    }

    public GitLabPushCause(GitLabPushRequest pushRequest, String pollingLog) {
        super(pollingLog);
        this.pushRequest=pushRequest;
    }

    public GitLabPushRequest getPushRequest() {
        return pushRequest;
    }

    @Override
    public String getShortDescription() {
        String pushedBy;
        if (pushRequest.getCommits().size() > 0){
            pushedBy = pushRequest.getCommits().get(0).getAuthor().getName();
        } else {
            pushedBy = pushRequest.getUser_name();
        }

        if (pushedBy == null) {
            return "Started by GitLab push";
        } else {
            return String.format("Started by GitLab push by %s", pushedBy);
        }
    }
}
