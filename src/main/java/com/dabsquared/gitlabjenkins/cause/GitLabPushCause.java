package com.dabsquared.gitlabjenkins.cause;

import com.dabsquared.gitlabjenkins.model.PushHook;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Run;

import java.io.File;
import java.io.IOException;

/**
 * @author Robin Müller
 */
public class GitLabPushCause extends GitLabWebHookCause<PushHook> {

    public GitLabPushCause(PushHook pushHook) {
        this(pushHook, "");
    }

    public GitLabPushCause(PushHook pushHook, File logFile) throws IOException {
        super(pushHook, logFile);
    }

    public GitLabPushCause(PushHook pushHook, String pollingLog) {
        super(pushHook, pollingLog);
    }

    @Override
    public String getBranch() {
        return getRequest().getRef().replaceFirst("^refs/heads/", "");
    }

    @Override
    public String getSourceBranch() {
        return getBranch();
    }

    @Override
    public ActionType getActionType() {
        return ActionType.PUSH;
    }

    @Override
    public String getUserName() {
        return getRequest().getUserName();
    }

    @Override
    public String getUserEmail() {
        return getRequest().getUserEmail();
    }

    @Override
    public String getSourceRepoHomepage() {
        return getRequest().getProject().getHomepage();
    }

    @Override
    public String getSourceRepoName() {
        return getRequest().getProject().getName();
    }

    @Override
    public String getSourceRepoUrl() {
        return getRequest().getProject().getUrl();
    }

    @Override
    public String getSourceRepoSshUrl() {
        return getRequest().getProject().getSshUrl();
    }

    @Override
    public String getSourceRepoHttpUrl() {
        return getRequest().getProject().getHttpUrl();
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
            return getRequest().getUserName();
        }
    }

    @Initializer(before = InitMilestone.PLUGINS_STARTED)
    public static void addAliases() {
        Run.XSTREAM2.addCompatibilityAlias("com.dabsquared.gitlabjenkins.GitLabPushCause", GitLabPushCause.class);
    }
}
