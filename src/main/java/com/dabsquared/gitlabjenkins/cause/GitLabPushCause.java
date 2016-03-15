package com.dabsquared.gitlabjenkins.cause;

import com.dabsquared.gitlabjenkins.model.Commit;
import com.dabsquared.gitlabjenkins.model.PushHook;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Run;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
        return getRequest().optRef().or("").replaceFirst("^refs/heads/", "");
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
        return getRequest().optUserName().orNull();
    }

    @Override
    public String getUserEmail() {
        return getRequest().optUserEmail().orNull();
    }

    @Override
    public String getSourceRepoHomepage() {
        return getRequest().getProject().optHomepage().orNull();
    }

    @Override
    public String getSourceRepoName() {
        return getRequest().getProject().optName().orNull();
    }

    @Override
    public String getSourceRepoUrl() {
        return getRequest().getProject().optUrl().orNull();
    }

    @Override
    public String getSourceRepoSshUrl() {
        return getRequest().getProject().optSshUrl().orNull();
    }

    @Override
    public String getSourceRepoHttpUrl() {
        return getRequest().getProject().optHttpUrl().orNull();
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
        List<Commit> commits = getRequest().getCommits();
        if (commits.size() > 0) {
            return commits.get(0).getAuthor().optName().orNull();
        } else {
            return getRequest().optUserName().orNull();
        }
    }

    @Initializer(before = InitMilestone.PLUGINS_STARTED)
    public static void addAliases() {
        Run.XSTREAM2.addCompatibilityAlias("com.dabsquared.gitlabjenkins.GitLabPushCause", GitLabPushCause.class);
    }
}
