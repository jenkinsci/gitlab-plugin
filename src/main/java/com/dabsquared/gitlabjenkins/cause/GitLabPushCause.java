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
    protected String getBranch() {
        return getRequest().getRef().replaceFirst("^refs/heads/", "");
    }

    @Override
    protected String getSourceBranch() {
        return getBranch();
    }

    @Override
    protected ActionType getActionType() {
        return ActionType.PUSH;
    }

    @Override
    protected String getUserName() {
        return getRequest().getUserName();
    }

    @Override
    protected String getUserEmail() {
        return getRequest().getUserEmail();
    }

    @Override
    protected String getSourceRepoHomepage() {
        return getRequest().getProject().getHomepage();
    }

    @Override
    protected String getSourceRepoName() {
        return getRequest().getProject().getName();
    }

    @Override
    protected String getSourceRepoUrl() {
        return getRequest().getProject().getUrl();
    }

    @Override
    protected String getSourceRepoSshUrl() {
        return getRequest().getProject().getSshUrl();
    }

    @Override
    protected String getSourceRepoHttpUrl() {
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
