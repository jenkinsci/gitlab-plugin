package com.dabsquared.gitlabjenkins.util;

import static com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty.getClient;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.workflow.GitLabBranchBuild;
import hudson.EnvVars;
import hudson.model.*;
import hudson.model.Cause.UpstreamCause;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.gitlab4j.api.Constants.CommitBuildState;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.CommitStatus;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;

/**
 * @author Robin Müller
 */
public class CommitStatusUpdater {

    private static final Logger LOGGER = Logger.getLogger(CommitStatusUpdater.class.getName());

    public static void updateCommitStatus(
            Run<?, ?> build,
            TaskListener listener,
            CommitBuildState state,
            String name,
            List<GitLabBranchBuild> gitLabBranchBuilds,
            GitLabConnectionProperty connection) {
        GitLabApi client;
        if (connection != null) {
            client = connection.getClient();
        } else {
            client = getClient(build);
        }

        if (client == null) {
            println(listener, "No GitLab connection configured");
            return;
        }

        EnvVars environment = null;
        if (gitLabBranchBuilds == null || gitLabBranchBuilds.isEmpty()) {
            try {
                environment = build.getEnvironment(listener);
                if (!environment.isEmpty()) {
                    gitLabBranchBuilds = retrieveGitlabProjectIds(build, environment);
                }
            } catch (IOException | InterruptedException e) {
                printf(listener, "Failed to get Gitlab Build list to update status: %s%n", e.getMessage());
            }
        }

        final String buildUrl = getBuildUrl(build);
        if (gitLabBranchBuilds != null) {
            for (final GitLabBranchBuild gitLabBranchBuild : gitLabBranchBuilds) {
                try {
                    GitLabApi current_client = client;
                    if (gitLabBranchBuild.getConnection() != null) {
                        GitLabApi build_specific_client =
                                gitLabBranchBuild.getConnection().getClient();
                        if (build_specific_client != null) {
                            current_client = build_specific_client;
                        }
                    }

                    String current_build_name = name;
                    if (gitLabBranchBuild.getName() != null) {
                        current_build_name = gitLabBranchBuild.getName();
                    }

                    if (existsCommit(
                            current_client, gitLabBranchBuild.getProjectId(), gitLabBranchBuild.getRevisionHash())) {
                        CommitStatus status = new CommitStatus();
                        status.withTargetUrl(buildUrl)
                                .withName(current_build_name)
                                .withDescription(build.getDisplayName())
                                .withRef(getBuildBranchOrTag(build, environment))
                                .withCoverage(null);
                        LOGGER.log(
                                Level.INFO,
                                String.format("Updating build '%s' to '%s'", gitLabBranchBuild.getProjectId(), state));
                        current_client
                                .getCommitsApi()
                                .addCommitStatus(
                                        gitLabBranchBuild.getProjectId(),
                                        gitLabBranchBuild.getRevisionHash(),
                                        state,
                                        status);
                    }
                } catch (GitLabApiException e) {
                    printf(
                            listener,
                            "Failed to update Gitlab commit status for project '%s': %s%n",
                            gitLabBranchBuild.getProjectId(),
                            e.getMessage());
                    LOGGER.log(
                            Level.SEVERE,
                            String.format(
                                    "Failed to update Gitlab commit status for project '%s'",
                                    gitLabBranchBuild.getProjectId()),
                            e);
                }
            }
        }
    }

    public static void updateCommitStatus(Run<?, ?> build, TaskListener listener, CommitBuildState state, String name) {
        try {
            updateCommitStatus(build, listener, state, name, null, null);
        } catch (IllegalStateException e) {
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
            LOGGER.log(
                    Level.FINE, "failed to print message {0} due to null TaskListener", String.format(message, args));
        } else {
            listener.getLogger().printf(message, args);
        }
    }

    private static boolean existsCommit(GitLabApi client, String gitlabProjectId, String commitHash) {
        try {
            client.getCommitsApi().getCommit(gitlabProjectId, commitHash);
            return true;
        } catch (GitLabApiException e) {
            LOGGER.log(
                    Level.FINE,
                    String.format("Project (%s) and commit (%s) combination not found", gitlabProjectId, commitHash));
            return false;
        }
    }

    private static String getBuildBranchOrTag(Run<?, ?> build, EnvVars environment) {
        GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);
        if (cause == null) {
            return environment == null ? null : environment.get("BRANCH_NAME", null);
        }
        if (cause.getData().getActionType() == CauseData.ActionType.TAG_PUSH) {
            return StringUtils.removeStart(cause.getData().getSourceBranch(), "refs/tags/");
        }
        return cause.getData().getSourceBranch();
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
                    gitlabCause.getData().getSourceProjectId().toString(),
                    gitlabCause.getData().getLastCommit()));
        }

        // Check upstream causes for GitLabWebHookCause
        List<GitLabBranchBuild> builds = findBuildsFromUpstreamCauses(build.getCauses());
        if (!builds.isEmpty()) {
            return builds;
        }

        final GitLabApi gitLabClient = getClient(build);
        if (gitLabClient == null) {
            LOGGER.log(Level.WARNING, "No gitlab client found.");
            return result;
        }

        final List<BuildData> buildDataList = build.getActions(BuildData.class);
        if (CollectionUtils.isEmpty(buildDataList)) {
            LOGGER.log(Level.INFO, "Build does not contain build data.");
            return result;
        }

        if (buildDataList.size() == 1) {
            addGitLabBranchBuild(
                    result, getBuildRevision(build), buildDataList.get(0).getRemoteUrls(), environment, gitLabClient);
        } else {
            final SCMRevisionAction scmRevisionAction = build.getAction(SCMRevisionAction.class);

            if (scmRevisionAction == null) {
                LOGGER.log(Level.INFO, "Build does not contain SCM revision action.");
                return result;
            }

            final SCMRevision scmRevision = scmRevisionAction.getRevision();

            String scmRevisionHash = null;
            if (scmRevision instanceof AbstractGitSCMSource.SCMRevisionImpl) {
                if (scmRevision == null) {
                    LOGGER.log(Level.INFO, "Build does not contain SCM revision object.");
                    return result;
                }
                scmRevisionHash = ((AbstractGitSCMSource.SCMRevisionImpl) scmRevision).getHash();
                if (scmRevisionHash == null) {
                    LOGGER.log(Level.INFO, "Build does not contain SCM revision hash.");
                    return result;
                }

                for (final BuildData buildData : buildDataList) {
                    for (final Entry<String, Build> buildByBranchName :
                            buildData.getBuildsByBranchName().entrySet()) {
                        if (buildByBranchName.getValue().getSHA1() != null) {
                            if (buildByBranchName.getValue().getSHA1().equals(ObjectId.fromString(scmRevisionHash))) {
                                addGitLabBranchBuild(
                                        result, scmRevisionHash, buildData.getRemoteUrls(), environment, gitLabClient);
                            }
                        }
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

    private static void addGitLabBranchBuild(
            List<GitLabBranchBuild> result,
            String scmRevisionHash,
            Set<String> remoteUrls,
            EnvVars environment,
            GitLabApi gitLabClient) {
        for (String remoteUrl : remoteUrls) {
            try {
                LOGGER.log(Level.INFO, "Retrieving the gitlab project id from remote url {0}", remoteUrl);
                final String projectNameWithNameSpace =
                        ProjectIdUtil.retrieveProjectId(gitLabClient, environment.expand(remoteUrl));
                if (StringUtils.isNotBlank(projectNameWithNameSpace)) {
                    String projectId = projectNameWithNameSpace;
                    if (projectNameWithNameSpace.contains(".")) {
                        try {
                            projectId = gitLabClient
                                    .getProjectApi()
                                    .getProject(projectNameWithNameSpace)
                                    .getId()
                                    .toString();
                        } catch (GitLabApiException e) {
                            LOGGER.log(
                                    Level.SEVERE,
                                    String.format(
                                            "Failed to retrieve projectId for project '%s'", projectNameWithNameSpace),
                                    e);
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
                List<Cause> upCauses =
                        ((UpstreamCause) cause).getUpstreamCauses(); // Non null, returns empty list when none are set
                for (Cause upCause : upCauses) {
                    if (upCause instanceof GitLabWebHookCause) {
                        GitLabWebHookCause gitlabCause = (GitLabWebHookCause) upCause;
                        return Collections.singletonList(new GitLabBranchBuild(
                                gitlabCause.getData().getSourceProjectId().toString(),
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
}
