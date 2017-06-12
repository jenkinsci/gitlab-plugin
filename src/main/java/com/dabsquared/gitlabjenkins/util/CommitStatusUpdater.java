package com.dabsquared.gitlabjenkins.util;

import static com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty.getClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang.StringUtils;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;

import hudson.EnvVars;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Cause.UpstreamCause;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildData;
import jenkins.model.Jenkins;

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
            String commitHash = getBuildRevision(build);
            String buildUrl = getBuildUrl(build);

            for (String gitlabProjectId : retrieveGitlabProjectIds(build, build.getEnvironment(listener))) {
                try {
                    if (existsCommit(client, gitlabProjectId, commitHash)) {
                        client.changeBuildStatus(gitlabProjectId, commitHash, state, getBuildBranch(build), name, buildUrl, null);
                    }
                } catch (WebApplicationException | ProcessingException e) {
                    printf(listener, "Failed to update Gitlab commit status for project '%s': %s%n", gitlabProjectId, e.getMessage());
                    LOGGER.log(Level.SEVERE, String.format("Failed to update Gitlab commit status for project '%s'", gitlabProjectId), e);
                }
            }
        } catch (IOException | InterruptedException | IllegalStateException e) {
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
        GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);
        if (cause != null) {
            return cause.getData().getLastCommit();
        }

        BuildData action = build.getAction(BuildData.class);
        if (action == null) {
            throw new IllegalStateException("No (git-plugin) BuildData associated to current build");
        }
        Revision lastBuiltRevision = action.getLastBuiltRevision();

        if (lastBuiltRevision == null) {
            throw new IllegalStateException("Last build has no associated commit");
        }

        return action.getLastBuild(lastBuiltRevision.getSha1()).getMarked().getSha1String();
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

    private static String getBuildBranch(Run<?, ?> build) {
        GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);
        return cause == null ? null : cause.getData().getSourceBranch();
    }

    private static String getBuildUrl(Run<?, ?> build) {
        return Jenkins.getInstance().getRootUrl() + build.getUrl();
    }

    private static List<String> retrieveGitlabProjectIds(Run<?, ?> build, EnvVars environment) {
        LOGGER.log(Level.INFO, "Retrieving gitlab project ids");

        GitLabWebHookCause gitlabCause = build.getCause(GitLabWebHookCause.class);
        if (gitlabCause != null) {
            return Collections.singletonList(gitlabCause.getData().getSourceProjectId().toString());
        }
        
        // Check upstream causes for GitLabWebHookCause
        for (Cause cause : build.getCauses()) {
        	if (cause instanceof UpstreamCause) {
        		for (Cause upCause : ((UpstreamCause) cause).getUpstreamCauses()) {
        			if (upCause instanceof GitLabWebHookCause) {
        			    return Collections.singletonList(((GitLabWebHookCause) upCause).getData().getSourceProjectId().toString());
        			}
        		}
        	}
        }

        List<String> result = new ArrayList<>();
        GitLabApi gitLabClient = getClient(build);
        if (gitLabClient == null) {
            LOGGER.log(Level.WARNING, "No gitlab client found.");
            return result;
        }

        final BuildData buildData = build.getAction(BuildData.class);
        if (buildData == null) {
            LOGGER.log(Level.INFO, "Build does not contain build data.");
            return result;
        }

        final Set<String> remoteUrls = buildData.getRemoteUrls();
        for (String remoteUrl : remoteUrls) {
            try {
                LOGGER.log(Level.INFO, "Retrieving the gitlab project id from remote url {0}", remoteUrl);
                final String projectNameWithNameSpace = ProjectIdUtil.retrieveProjectId(environment.expand(remoteUrl));
                if (StringUtils.isNotBlank(projectNameWithNameSpace)) {
                    String projectId = projectNameWithNameSpace;
                    if (projectNameWithNameSpace.contains(".")) {
                        try {
                            projectId = gitLabClient.getProject(projectNameWithNameSpace).getId().toString();
                        } catch (WebApplicationException | ProcessingException e) {
                            LOGGER.log(Level.SEVERE, String.format("Failed to retrieve projectId for project '%s'", projectNameWithNameSpace), e);
                        }
                    }
                    result.add(projectId);
                }
            } catch (ProjectIdUtil.ProjectIdResolutionException e) {
                // nothing to do
            }
        }
        return result;
    }

}
