package com.dabsquared.gitlabjenkins.util;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.util.BuildData;
import jenkins.model.Jenkins;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Robin Müller
 */
public class CommitStatusUpdater {

    private final static Logger LOGGER = Logger.getLogger(CommitStatusUpdater.class.getName());

    public static void updateCommitStatus(Run<?, ?> build, TaskListener listener, BuildState state) {
        String commitHash = getBuildRevision(build);
        String buildUrl = getBuildUrl(build);
        try {
            for (String gitlabProjectId : retrieveGitlabProjectIds(build, build.getEnvironment(listener))) {
                try {
                    GitLabApi client = getClient(build);
                    if (client == null) {
                        println(listener, "No GitLab connection configured");
                    } else {
                        client.changeBuildStatus(gitlabProjectId, commitHash, state, getBuildBranch(build), "jenkins", buildUrl, null);
                    }
                } catch (WebApplicationException e) {
                    printf(listener, "Failed to update Gitlab commit status for project '%s': %s%n", gitlabProjectId, e.getMessage());
                    LOGGER.log(Level.SEVERE, String.format("Failed to update Gitlab commit status for project '%s'", gitlabProjectId), e);
                }
            }
        } catch (IOException | InterruptedException e) {
            printf(listener, "Failed to update Gitlab commit status: %s%n", e.getMessage());
        }
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

    private static String getBuildRevision(Run<?, ?> build) {
        return build.getAction(BuildData.class).lastBuild.marked.getSha1String();
    }

    private static String getBuildBranch(Run<?, ?> build) {
        GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);
        return cause == null ? null : cause.getData().getSourceBranch();
    }

    private static String getBuildUrl(Run<?, ?> build) {
        return Jenkins.getInstance().getRootUrl() + build.getUrl();
    }

    private static GitLabApi getClient(Run<?, ?> build) {
        GitLabConnectionProperty connectionProperty = build.getParent().getProperty(GitLabConnectionProperty.class);
        if (connectionProperty != null) {
            return connectionProperty.getClient();
        }
        return null;
    }

    private static List<String> retrieveGitlabProjectIds(Run<?, ?> build, EnvVars environment) {
        List<String> result = new ArrayList<>();
        for (String remoteUrl : build.getAction(BuildData.class).getRemoteUrls()) {
            try {
                result.add(ProjectIdUtil.retrieveProjectId(environment.expand(remoteUrl)));
            } catch (ProjectIdUtil.ProjectIdResolutionException e) {
                // nothing to do
            }
        }
        return result;
    }

}
