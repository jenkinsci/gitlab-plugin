package com.dabsquared.gitlabjenkins.models.cause;

import com.dabsquared.gitlabjenkins.models.request.GitLabMergeRequest;

import java.io.File;
import java.io.IOException;

public class GitLabMergeCause extends GitLabCause<GitLabMergeRequest> {

    public GitLabMergeCause(GitLabMergeRequest request, File logFile) throws IOException {
        super(request, logFile);
    }

    public GitLabMergeCause(GitLabMergeRequest request, String pollingLog) {
        super(request, pollingLog);
    }

    public GitLabMergeCause(GitLabMergeRequest req) {
        super(req);
    }

    @Override
    public String getShortDescription() {
        return "GitLab Merge Request #" + this.request.getObjectAttribute().getIid() + " : " + this.request.getObjectAttribute().getSourceBranch() +
            " => " + this.request.getObjectAttribute().getTargetBranch();
    }
}
