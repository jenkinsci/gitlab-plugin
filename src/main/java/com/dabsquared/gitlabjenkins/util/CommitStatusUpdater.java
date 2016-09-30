package com.dabsquared.gitlabjenkins.util;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty.getClient;

/**
 * @author Robin Müller
 */
public class CommitStatusUpdater {

    private final static Logger LOGGER = Logger.getLogger(CommitStatusUpdater.class.getName());

    public static void updateCommitStatus(Run<?, ?> build, TaskListener listener, BuildState state, String name) {
        GitLabApi client = getClient(build);
        if (client == null) {
            println(listener, "No GitLab connection configured");
            return;
        }

        try {
            boolean causeFound = false;
            String buildUrl = getBuildUrl(build);
            for (Cause cause : build.getCauses()) {
                if (cause instanceof GitLabWebHookCause) {
                    causeFound = true;
                    GitLabWebHookCause gitLabCause = (GitLabWebHookCause) cause;
                    String gitlabProjectId = getProjectId(gitLabCause);
                    String commitHash = gitLabCause.getData().getLastCommit();
                    String branch = gitLabCause.getData().getSourceBranch();
                    try {
                        if (existsCommit(client, gitlabProjectId, commitHash)) {
                            client.changeBuildStatus(gitlabProjectId, commitHash, state, branch, name, buildUrl, null);
                        }
                    } catch (WebApplicationException | ProcessingException e) {
                        printf(listener, "Failed to update Gitlab commit status for project '%s': %s%n", gitlabProjectId, e.getMessage());
                        LOGGER.log(Level.SEVERE, String.format("Failed to update Gitlab commit status for project '%s'", gitlabProjectId), e);
                    }
                }
            }
            if (!causeFound) {
                throw new IllegalStateException("no associated GitLabWebHookCause");
            }
        } catch (IllegalStateException e) {
            printf(listener, "Failed to update Gitlab commit status: %s%n", e.getMessage());
        }
    }

    private static String getProjectId(GitLabWebHookCause gitLabCause) {
        CauseData data = gitLabCause.getData();
        String sourceRepoName = data.getSourceRepoName();
        if (sourceRepoName.contains(".")) return data.getSourceProjectId().toString();
        return sourceRepoName;
    }
    
    private static void println(TaskListener listener, String message) {
        if (listener == null) {
            LOGGER.log(Level.FINE, "failed to print message {0} due to null TaskListener", message);
        } else {
            listener.getLogger().println(message);
        }
    }

    private static void printf(TaskListener listener, String message, Object... args) {
        if (listener == null) {
            LOGGER.log(Level.FINE, "failed to print message {0} due to null TaskListener", String.format(message, args));
        } else {
            listener.getLogger().printf(message, args);
        }
    }
    
    private static boolean existsCommit(GitLabApi client, String gitlabProjectId, String commitHash) {
        try {
            client.getCommit(gitlabProjectId, commitHash);
            return true;
        } catch (NotFoundException e) {
            LOGGER.log(Level.FINE, String.format("Project (%s) and commit (%s) combination not found", gitlabProjectId, commitHash));
            return false;
        }
    }
    
    private static String getBuildUrl(Run<?, ?> build) {
        return Jenkins.getInstance().getRootUrl() + build.getUrl();
    }

}
