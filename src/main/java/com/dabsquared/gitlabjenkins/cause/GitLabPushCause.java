package com.dabsquared.gitlabjenkins.cause;

import com.dabsquared.gitlabjenkins.GitLabPushRequest;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Run;

import java.io.File;
import java.io.IOException;

/**
 * Created by daniel on 6/8/14.
 */
public class GitLabPushCause extends GitLabWebHookCause<GitLabPushRequest> {

    private transient GitLabPushRequest pushRequest;

    public GitLabPushCause(GitLabPushRequest pushRequest) {
        this(pushRequest, "");
    }

    public GitLabPushCause(GitLabPushRequest pushRequest, File logFile) throws IOException {
        super(pushRequest, logFile);
    }

    public GitLabPushCause(GitLabPushRequest pushRequest, String pollingLog) {
        super(pushRequest, pollingLog);
    }

    @Override
    public String getShortDescription() {
        String pushedBy = retrievePushedBy();
        if (pushedBy == null) {
            return "Started by GitLab push";
        } else {
            return String.format("Started by GitLab push by %s", pushedBy);
        }
    }

    private String retrievePushedBy() {
        if (getRequest().getCommits().size() > 0) {
            return getRequest().getCommits().get(0).getAuthor().getName();
        } else {
            return getRequest().getUser_name();
        }
    }

    @Initializer(before = InitMilestone.PLUGINS_STARTED)
    public static void addAliases() {
        Run.XSTREAM2.addCompatibilityAlias("com.dabsquared.gitlabjenkins.GitLabPushCause", GitLabPushCause.class);
    }

    protected Object readResolve() {
        if (getRequest() == null) {
            return new GitLabPushCause(pushRequest);
        } else {
            return this;
        }
    }
}
