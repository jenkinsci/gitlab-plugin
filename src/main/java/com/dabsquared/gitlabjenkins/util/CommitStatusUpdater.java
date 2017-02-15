package com.dabsquared.gitlabjenkins.util;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;

import hudson.EnvVars;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildData;
import jenkins.model.Jenkins;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty.getClient;

/**
 * @author Robin Müller
 */
public class CommitStatusUpdater {

    private static class CommitStatusInfo {
        
        private final String projectId;
        private final String commitHash;
        private final String branchNameOrNull;

        public CommitStatusInfo(String projectId, String commitHash, String branchNameOrNull) {
            this.projectId = projectId;
            this.commitHash = commitHash;
            this.branchNameOrNull = branchNameOrNull;
        }
        
        public String getProjectId() {
            return projectId;
        }
        
        public String getCommitHash() {
            return commitHash;
        }
        
        public String getBranchNameOrNull() {
            return branchNameOrNull;
        }
    }
    
    private final static Logger LOGGER = Logger.getLogger(CommitStatusUpdater.class.getName());

    public static void updateCommitStatus(Run<?, ?> build, TaskListener listener, BuildState state, String name){
        GitLabApi client = getClient(build);
        if (client == null) {
            println(listener, "No GitLab connection configured");
            return;
        }

        try {
            String buildUrl = getBuildUrl(build);
            Collection<CommitStatusInfo> statusInfoList = collectCommitStatusInfo(client, build, listener);

            if (statusInfoList.isEmpty()) {
                throw new IllegalStateException("no associated GitLabWebHookCause");
            }
            
            for (CommitStatusInfo info : statusInfoList) {
                String projectId = info.getProjectId();
                String commitHash = info.getCommitHash();
                String branchNameOrNull = info.getBranchNameOrNull();
                try {
                    if (existsCommit(client, projectId, commitHash)) {
                        client.changeBuildStatus(projectId, commitHash, state, branchNameOrNull, name, buildUrl, null);
                    }
                } catch (WebApplicationException | ProcessingException e) {
                    printf(listener, "Failed to update Gitlab commit status for project '%s': %s%n", projectId, e.getMessage());
                    LOGGER.log(Level.SEVERE, String.format("Failed to update Gitlab commit status for project '%s'", projectId), e);
                }
            }
        } catch (IllegalStateException e) {
            printf(listener, "Failed to update Gitlab commit status: %s%n", e.getMessage());
        }
    }

    private static List<CommitStatusInfo> collectCommitStatusInfo(GitLabApi client, Run<?, ?> build, TaskListener listener) {
        ArrayList<CommitStatusInfo> statusInfoList = new ArrayList<>();
        for (Cause cause : build.getCauses()) {
            if (cause instanceof GitLabWebHookCause) {
                GitLabWebHookCause gitLabCause = (GitLabWebHookCause) cause;
                String projectId = getProjectId(gitLabCause);
                String commitHash = gitLabCause.getData().getLastCommit();
                String branchName = gitLabCause.getData().getSourceBranch();
                statusInfoList.add(new CommitStatusInfo(projectId, commitHash, branchName));
            }
        }
        if (statusInfoList.isEmpty()) {
            return collectCommitStatusInfoFromBuildData(client, build, listener);
        }
        return statusInfoList;
    }

    private static List<CommitStatusInfo> collectCommitStatusInfoFromBuildData(GitLabApi client, Run<?, ?> build,  TaskListener listener) {
        try {
            ArrayList<CommitStatusInfo> statusInfoList = new ArrayList<>();
            String commitHash = getBuildRevisionFromBuildData(build);
            for (String projectId : getProjectIds(client, build, build.getEnvironment(listener))) {
                statusInfoList.add(new CommitStatusInfo(projectId, commitHash, null));
            }
            return statusInfoList;
        } catch (IOException | InterruptedException e) {
            printf(listener, "Failed to update Gitlab commit status: %s%n", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static String getBuildRevisionFromBuildData(Run<?, ?> build) {
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
    
    private static List<String> getProjectIds(GitLabApi client, Run<?, ?> build, EnvVars environment) {
        LOGGER.log(Level.INFO, "Retrieving gitlab project ids");

        List<String> result = new ArrayList<>();
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
                            projectId = client.getProject(projectNameWithNameSpace).getId().toString();
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
