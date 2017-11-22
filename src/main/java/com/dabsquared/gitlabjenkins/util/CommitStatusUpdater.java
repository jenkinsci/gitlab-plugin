package com.dabsquared.gitlabjenkins.util;


import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import hudson.EnvVars;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Cause.UpstreamCause;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import jenkins.model.Jenkins;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty.getClient;

/**
 * @author Robin Müller
 */
public class CommitStatusUpdater {

    private final static Logger LOGGER = Logger.getLogger(CommitStatusUpdater.class.getName());

    public static void updateCommitStatus(Run<?, ?> build, TaskListener listener, BuildState state, String name) {
        GitLabClient client = getClient(build);
        if (client == null) {
            println(listener, "No GitLab connection configured");
            return;
        }

        try {
            final String buildUrl = getBuildUrl(build);

            for (final GitLabBranchBuild gitLabBranchBuild : retrieveGitlabProjectIds(build, build.getEnvironment(listener))) {
                try {
                    if (existsCommit(client, gitLabBranchBuild.getProjectId(), gitLabBranchBuild.getRevisionHash())) {
                        client.changeBuildStatus(gitLabBranchBuild.getProjectId(), gitLabBranchBuild.getRevisionHash(), state, getBuildBranch(build), name, buildUrl, state.name());
                    }
                } catch (WebApplicationException | ProcessingException e) {
                    printf(listener, "Failed to update Gitlab commit status for project '%s': %s%n", gitLabBranchBuild.getProjectId(), e.getMessage());
                    LOGGER.log(Level.SEVERE, String.format("Failed to update Gitlab commit status for project '%s'", gitLabBranchBuild.getProjectId()), e);
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

    private static boolean existsCommit(GitLabClient client, String gitlabProjectId, String commitHash) {
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
        return DisplayURLProvider.get().getRunURL(build);
    }

    private static List<GitLabBranchBuild> retrieveGitlabProjectIds(Run<?, ?> build, EnvVars environment) {
        LOGGER.log(Level.INFO, "Retrieving gitlab project ids");
        final List<GitLabBranchBuild> result = new ArrayList<>();

        GitLabWebHookCause gitlabCause = build.getCause(GitLabWebHookCause.class);
        if (gitlabCause != null) {
            return Collections.singletonList(new GitLabBranchBuild(
                    gitlabCause.getData().getSourceProjectId().toString(), gitlabCause.getData().getLastCommit()));
        }

        // Check upstream causes for GitLabWebHookCause
        List<GitLabBranchBuild> builds = findBuildsFromUpstreamCauses(build.getCauses());
        if (!builds.isEmpty()) {
            return builds;
        }

        final GitLabClient gitLabClient = getClient(build);
        if (gitLabClient == null) {
            LOGGER.log(Level.WARNING, "No gitlab client found.");
            return result;
        }

        final List<BuildData> buildDatas = build.getActions(BuildData.class);
        if (CollectionUtils.isEmpty(buildDatas)) {
            LOGGER.log(Level.INFO, "Build does not contain build data.");
            return result;
        }

        if (buildDatas.size() == 1) {
            addGitLabBranchBuild(result, getBuildRevision(build), buildDatas.get(0).getRemoteUrls(), environment, gitLabClient);
        } else {
            final SCMRevisionAction scmRevisionAction = build.getAction(SCMRevisionAction.class);

            if (scmRevisionAction == null) {
                LOGGER.log(Level.INFO, "Build does not contain SCM revision action.");
                return result;
            }

            final SCMRevision scmRevision = scmRevisionAction.getRevision();

            String scmRevisionHash = null;
            if (scmRevision instanceof AbstractGitSCMSource.SCMRevisionImpl) {
                scmRevisionHash = ((AbstractGitSCMSource.SCMRevisionImpl) scmRevision).getHash();
            }

            for (final BuildData buildData : buildDatas) {
                for (final Entry<String, Build> buildByBranchName : buildData.getBuildsByBranchName().entrySet()) {
                    if (buildByBranchName.getValue().getSHA1().equals(ObjectId.fromString(scmRevisionHash))) {
                        addGitLabBranchBuild(result, scmRevisionHash, buildData.getRemoteUrls(), environment, gitLabClient);
                    }
                }
            }
        }

        return result;
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

	private static void addGitLabBranchBuild(List<GitLabBranchBuild> result, String scmRevisionHash,
                                             Set<String> remoteUrls, EnvVars environment, GitLabClient gitLabClient) {
        for (String remoteUrl : remoteUrls) {
            try {
                LOGGER.log(Level.INFO, "Retrieving the gitlab project id from remote url {0}", remoteUrl);
                final String projectNameWithNameSpace = ProjectIdUtil.retrieveProjectId(gitLabClient, environment.expand(remoteUrl));
                if (StringUtils.isNotBlank(projectNameWithNameSpace)) {
                    String projectId = projectNameWithNameSpace;
                    if (projectNameWithNameSpace.contains(".")) {
                        try {
                            projectId = gitLabClient.getProject(projectNameWithNameSpace).getId().toString();
                        } catch (WebApplicationException | ProcessingException e) {
                            LOGGER.log(Level.SEVERE, String.format("Failed to retrieve projectId for project '%s'",
                                projectNameWithNameSpace), e);
                        }
                    }
                    result.add(new GitLabBranchBuild(projectId, scmRevisionHash));
                }
            } catch (ProjectIdUtil.ProjectIdResolutionException e) {
                LOGGER.log(Level.WARNING, "Did not match project id in remote url.");
            }
        }
    }

    private static List<GitLabBranchBuild> findBuildsFromUpstreamCauses(List<Cause> causes) {
        for (Cause cause : causes) {
            if (cause instanceof UpstreamCause) {
                List<Cause> upCauses = ((UpstreamCause) cause).getUpstreamCauses();    // Non null, returns empty list when none are set
                for (Cause upCause : upCauses) {
                    if (upCause instanceof GitLabWebHookCause) {
                        GitLabWebHookCause gitlabCause = (GitLabWebHookCause) upCause;
                        return Collections.singletonList(
                                new GitLabBranchBuild(gitlabCause.getData().getSourceProjectId().toString(),
                                        gitlabCause.getData().getLastCommit()));
                    }
                }
                List<GitLabBranchBuild> builds = findBuildsFromUpstreamCauses(upCauses);
                if (!builds.isEmpty()) {
                    return builds;
                }
            }
        }
        return Collections.emptyList();
    }

    public static class GitLabBranchBuild {
        private final String projectId;
        private final String revisionHash;

        public GitLabBranchBuild(final String projectId, final String revisionHash) {
            this.projectId = projectId;
            this.revisionHash = revisionHash;
        }

        public String getProjectId() {
            return this.projectId;
        }

        public String getRevisionHash() {
            return this.revisionHash;
        }
    }
}
