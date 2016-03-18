package com.dabsquared.gitlabjenkins.cause;

import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.api.model.ObjectAttributes;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Run;

import java.io.File;
import java.io.IOException;

/**
 * @author Robin Müller
 */
public class GitLabMergeCause extends GitLabWebHookCause<MergeRequestHook> {

    public GitLabMergeCause(MergeRequestHook mergeRequestHook) {
        this(mergeRequestHook, "");
    }

    public GitLabMergeCause(MergeRequestHook mergeRequestHook, String pollingLog) {
        super(mergeRequestHook, pollingLog);
    }

    public GitLabMergeCause(MergeRequestHook mergeRequestHook, File logFile) throws IOException {
        super(mergeRequestHook, logFile);
    }

    @Override
    public String getBranch() {
        return getObjectAttributes().optSourceBranch().orNull();
    }

    @Override
    public String getSourceBranch() {
        return getObjectAttributes().optSourceBranch().orNull();
    }

    @Override
    public GitLabWebHookCause.ActionType getActionType() {
        return ActionType.MERGE;
    }

    @Override
    public String getUserName() {
        return getObjectAttributes().getLastCommit().getAuthor().optName().orNull();
    }

    @Override
    public String getUserEmail() {
        return getObjectAttributes().getLastCommit().getAuthor().optEmail().orNull();
    }

    @Override
    public String getSourceRepoHomepage() {
        return getObjectAttributes().getSource().optHomepage().orNull();
    }

    @Override
    public String getSourceRepoName() {
        return getObjectAttributes().getSource().optName().orNull();
    }

    @Override
    public String getSourceRepoUrl() {
        return getObjectAttributes().getSource().optUrl().orNull();
    }

    @Override
    public String getSourceRepoSshUrl() {
        return getObjectAttributes().getSource().optSshUrl().orNull();
    }

    @Override
    public String getSourceRepoHttpUrl() {
        return getObjectAttributes().getSource().optHttpUrl().orNull();
    }

    @Override
    public String getShortDescription() {
        ObjectAttributes objectAttribute = getObjectAttributes();
        return "GitLab Merge Request #" + objectAttribute.optIid().orNull() + " : " + objectAttribute.optSourceBranch().orNull() +
                " => " + objectAttribute.optTargetBranch().orNull();
    }

    @Override
    public String getMergeRequestTitle() {
        return getObjectAttributes().optTitle().orNull();
    }

    @Override
    public String getMergeRequestDescription() {
        return getObjectAttributes().optDescription().orNull();
    }

    @Override
    public String getMergeRequestId() {
        return getObjectAttributes().optId().isPresent() ? getObjectAttributes().optId().toString() : null;
    }

    @Override
    public String getTargetBranch() {
        return getObjectAttributes().optTargetBranch().orNull();
    }

    @Override
    public String getTargetRepoName() {
        return getObjectAttributes().getTarget().optName().orNull();
    }

    @Override
    public String getTargetRepoSshUrl() {
        return getObjectAttributes().getTarget().optSshUrl().orNull();
    }

    @Override
    public String getTargetRepoHttpUrl() {
        return getObjectAttributes().getTarget().optHttpUrl().orNull();
    }

    private ObjectAttributes getObjectAttributes() {
        return getRequest().getObjectAttributes();
    }


    @Initializer(before = InitMilestone.PLUGINS_STARTED)
    public static void addAliases() {
        Run.XSTREAM2.addCompatibilityAlias("com.dabsquared.gitlabjenkins.GitLabMergeCause", GitLabMergeCause.class);
    }
}
