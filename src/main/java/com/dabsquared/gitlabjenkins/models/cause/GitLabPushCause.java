package com.dabsquared.gitlabjenkins.models.cause;

import com.dabsquared.gitlabjenkins.models.request.GitLabPushRequest;

import java.io.File;
import java.io.IOException;

public class GitLabPushCause extends GitLabCause<GitLabPushRequest> {

    public GitLabPushCause(GitLabPushRequest request, File logFile) throws IOException {
        super(request, logFile);
    }

    public GitLabPushCause(GitLabPushRequest request, String pollingLog) {
        super(request, pollingLog);
    }

    public GitLabPushCause(GitLabPushRequest req) {
        super(req);
    }

    @Override
    public String getShortDescription() {
        String pushedBy;
        if (request.getCommits().size() > 0) {
            pushedBy = request.getCommits().get(0).getAuthor().getName();
        } else {
            pushedBy = request.getUser_name();
        }

        if (pushedBy == null) {
            return "Started by GitLab push";
        } else {
            return String.format("Started by GitLab push by %s", pushedBy);
        }
    }
}
