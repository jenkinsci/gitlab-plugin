package com.dabsquared.gitlabjenkins.cause;

import com.dabsquared.gitlabjenkins.GitLabMergeRequest;
import com.dabsquared.gitlabjenkins.data.ObjectAttributes;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Run;

import java.io.File;
import java.io.IOException;

/**
 * Created by daniel on 6/8/14.
 */
public class GitLabMergeCause extends GitLabWebHookCause<GitLabMergeRequest> {

    private transient GitLabMergeRequest mergeRequest;

    public GitLabMergeCause(GitLabMergeRequest mergeRequest) {
        this(mergeRequest, "");
    }

    public GitLabMergeCause(GitLabMergeRequest mergeRequest, File logFile) throws IOException {
        super(mergeRequest, logFile);
    }

    public GitLabMergeCause(GitLabMergeRequest mergeRequest, String pollingLog) {
        super(mergeRequest, pollingLog);
    }

    @Override
    public String getShortDescription() {
        ObjectAttributes objectAttribute = getRequest().getObjectAttribute();
        return "GitLab Merge Request #" + objectAttribute.getIid() + " : " + objectAttribute.getSourceBranch() +
                " => " + objectAttribute.getTargetBranch();
    }

    @Initializer(before = InitMilestone.PLUGINS_STARTED)
    public static void addAliases() {
        Run.XSTREAM2.addCompatibilityAlias("com.dabsquared.gitlabjenkins.GitLabMergeCause", GitLabMergeCause.class);
    }

    protected Object readResolve() {
        if (getRequest() == null) {
            return new GitLabMergeCause(mergeRequest);
        } else {
            return this;
        }
    }
}
