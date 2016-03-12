package com.dabsquared.gitlabjenkins.cause;

import com.dabsquared.gitlabjenkins.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.model.ObjectAttributes;
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
    protected String getBranch() {
        return getRequest().getObjectAttributes().getSourceBranch();
    }

    @Override
    protected String getSourceBranch() {
        return getRequest().getObjectAttributes().getSourceBranch();
    }

    @Override
    protected GitLabWebHookCause.ActionType getActionType() {
        return ActionType.MERGE;
    }

    @Override
    protected String getUserName() {
        return getRequest().getObjectAttributes().getLastCommit().getAuthor().getName();
    }

    @Override
    protected String getUserEmail() {
        return getRequest().getObjectAttributes().getLastCommit().getAuthor().getEmail();
    }

    @Override
    protected String getSourceRepoHomepage() {
        return getRequest().getObjectAttributes().getSource().getHomepage();
    }

    @Override
    protected String getSourceRepoName() {
        return getRequest().getObjectAttributes().getSource().getName();
    }

    @Override
    protected String getSourceRepoUrl() {
        return getRequest().getObjectAttributes().getSource().getUrl();
    }

    @Override
    protected String getSourceRepoSshUrl() {
        return getRequest().getObjectAttributes().getSource().getSshUrl();
    }

    @Override
    protected String getSourceRepoHttpUrl() {
        return getRequest().getObjectAttributes().getSource().getHttpUrl();
    }

    @Override
    public String getShortDescription() {
        ObjectAttributes objectAttribute = getRequest().getObjectAttributes();
        return "GitLab Merge Request #" + objectAttribute.getIid() + " : " + objectAttribute.getSourceBranch() +
                " => " + objectAttribute.getTargetBranch();
    }

    @Initializer(before = InitMilestone.PLUGINS_STARTED)
    public static void addAliases() {
        Run.XSTREAM2.addCompatibilityAlias("com.dabsquared.gitlabjenkins.GitLabMergeCause", GitLabMergeCause.class);
    }
}
