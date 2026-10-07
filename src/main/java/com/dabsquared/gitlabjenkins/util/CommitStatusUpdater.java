package com.dabsquared.gitlabjenkins.util;

import static com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty.getClient;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Pipeline;
import com.dabsquared.gitlabjenkins.workflow.GitLabBranchBuild;
import hudson.EnvVars;
import hudson.model.*;
import hudson.model.Cause.UpstreamCause;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;

/**
 * @author Robin Müller
 */
public class CommitStatusUpdater {

    private static final Logger LOGGER = Logger.getLogger(CommitStatusUpdater.class.getName());

    public static void updateCommitStatus(
            Run<?, ?> build,
            TaskListener listener,
            BuildState state,
            String name,
            List<GitLabBranchBuild> gitLabBranchBuilds,
            GitLabConnectionProperty connection) {
        updateCommitStatus(build, listener, state, name, gitLabBranchBuilds, connection, null);
    }

    /**
     * Same as the six-arg overload above, but lets the caller explicitly opt in or out of
     * attaching the status to the commit's merge request pipeline (see
     * resolveMergeRequestPipelineId), overriding the
     * {@link com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig} global
     * default for this one call. Pass null to just use that global default.
     */
    public static void updateCommitStatus(
            Run<?, ?> build,
            TaskListener listener,
            BuildState state,
            String name,
            List<GitLabBranchBuild> gitLabBranchBuilds,
            GitLabConnectionProperty connection,
            Boolean attachStatusToMergeRequestPipeline) {
        GitLabClient client;
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
                printf(listener, "Failed to get GitLab Build list to update status: %s%n", e.getMessage());
            }
        }

        final String buildUrl = getBuildUrl(build);
        if (gitLabBranchBuilds != null) {
            for (final GitLabBranchBuild gitLabBranchBuild : gitLabBranchBuilds) {
                try {
                    GitLabClient current_client = client;
                    if (gitLabBranchBuild.getConnection() != null) {
                        GitLabClient build_specific_client =
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
                        LOGGER.log(
                                Level.INFO,
                                "Updating build '%s' to '%s'".formatted(gitLabBranchBuild.getProjectId(), state));
                        Integer pipelineId = null;
                        if (shouldAttachStatusToMergeRequestPipeline(attachStatusToMergeRequestPipeline)) {
                            pipelineId = resolveMergeRequestPipelineId(
                                    current_client,
                                    gitLabBranchBuild.getProjectId(),
                                    gitLabBranchBuild.getRevisionHash());
                        }
                        // GitLab's Commit Status API requires pipeline_id, sha and ref to all
                        // agree on the SAME pipeline - confirmed empirically: passing a real,
                        // valid pipeline_id alongside a ref that belongs to a DIFFERENT
                        // pipeline for that sha (e.g. the branch name instead of a merge
                        // request pipeline's refs/merge-requests/N/head) gets rejected with
                        // "404 Pipeline for pipeline_id, sha and ref Not Found" even though
                        // the pipeline_id itself is entirely valid. Once we've resolved a
                        // pipeline_id, ref becomes redundant (and actively wrong, since the
                        // build's branch name never matches a merge request pipeline's own
                        // ref) - omit it so GitLab derives ref from the pipeline instead.
                        String ref = pipelineId == null ? getBuildBranchOrTag(build, environment) : null;
                        current_client.changeBuildStatus(
                                gitLabBranchBuild.getProjectId(),
                                gitLabBranchBuild.getRevisionHash(),
                                state,
                                ref,
                                current_build_name,
                                buildUrl,
                                state.name(),
                                pipelineId);
                    }
                } catch (WebApplicationException | ProcessingException e) {
                    // e.getMessage() alone (e.g. "HTTP 404 Not Found") is often too vague to
                    // debug from the build console - GitLab's actual JSON error body (e.g.
                    // "404 Pipeline for pipeline_id, sha and ref Not Found") is much more
                    // specific and is what actually diagnosed this class of bug. Read it
                    // once, defensively, since the response entity stream can only be
                    // consumed a single time and may not be a WebApplicationException at all.
                    String detail = e.getMessage();
                    if (e instanceof WebApplicationException wae && wae.getResponse() != null) {
                        try {
                            String body = wae.getResponse().readEntity(String.class);
                            if (StringUtils.isNotBlank(body)) {
                                detail = detail + " - " + body;
                            }
                        } catch (RuntimeException ignored) {
                            // response entity already consumed or unavailable; fall back to detail as-is
                        }
                    }
                    printf(
                            listener,
                            "Failed to update GitLab commit status for project '%s': %s%n",
                            gitLabBranchBuild.getProjectId(),
                            detail);
                    LOGGER.log(
                            Level.SEVERE,
                            "Failed to update GitLab commit status for project '%s'"
                                    .formatted(gitLabBranchBuild.getProjectId()),
                            e);
                }
            }
        }
    }

    public static void updateCommitStatus(Run<?, ?> build, TaskListener listener, BuildState state, String name) {
        updateCommitStatus(build, listener, state, name, (Boolean) null);
    }

    /**
     * Same as the four-arg overload above, but with an explicit
     * attachStatusToMergeRequestPipeline override; see the seven-arg overload.
     */
    public static void updateCommitStatus(
            Run<?, ?> build,
            TaskListener listener,
            BuildState state,
            String name,
            Boolean attachStatusToMergeRequestPipeline) {
        try {
            updateCommitStatus(build, listener, state, name, null, null, attachStatusToMergeRequestPipeline);
        } catch (IllegalStateException e) {
            printf(listener, "Failed to update GitLab commit status: %s%n", e.getMessage());
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
            LOGGER.log(Level.FINE, "failed to print message {0} due to null TaskListener", message.formatted(args));
        } else {
            listener.getLogger().printf(message, args);
        }
    }

    /**
     * Resolves whether this particular status update should be attached to the commit's
     * merge request pipeline: an explicit per-call override (from a gitlabCommitStatus /
     * updateGitlabCommitStatus step) wins if given, otherwise falls back to the plugin's
     * global default (GitLabConnectionConfig#isAttachStatusToMergeRequestPipeline, off
     * unless an administrator opts in).
     */
    private static boolean shouldAttachStatusToMergeRequestPipeline(Boolean attachStatusToMergeRequestPipelineOverride) {
        if (attachStatusToMergeRequestPipelineOverride != null) {
            return attachStatusToMergeRequestPipelineOverride;
        }
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            return false;
        }
        GitLabConnectionConfig config = (GitLabConnectionConfig) jenkins.getDescriptor(GitLabConnectionConfig.class);
        return config != null && config.isAttachStatusToMergeRequestPipeline();
    }

    /**
     * Looks up this commit's merge_request_event pipeline right now, so the status update
     * can target it explicitly instead of letting GitLab pick (or spin up a throwaway
     * "external" pipeline) based on sha+ref+context alone. Called fresh before EVERY status
     * update (not just the first), so a later call - e.g. "success", posted after GitLab's
     * own MR pipeline has since been created - still finds and targets the correct,
     * up-to-date pipeline, even though an earlier call (e.g. "pending") may have found
     * nothing yet and been left for GitLab to handle on its own via the old fallback path
     * (returning null here preserves that exact old behavior).
     *
     * Deliberately scoped to merge_request_event pipelines only, never a plain push
     * pipeline or GitLab's own throwaway "external" pipelines (the ones created by a status
     * update that had no real pipeline to attach to): the whole point of this is keeping a
     * build's result visible to the MR's head_pipeline, which a push pipeline isn't.
     */
    private static Integer resolveMergeRequestPipelineId(GitLabClient client, String projectId, String sha) {
        try {
            List<Pipeline> pipelines = client.getPipelines(projectId, sha);
            if (pipelines == null || pipelines.isEmpty()) {
                return null;
            }
            return pipelines.stream()
                    .filter(p -> "merge_request_event".equals(p.getSource()))
                    .max(Comparator.comparing(p -> p.getCreatedAt() == null ? "" : p.getCreatedAt()))
                    .map(Pipeline::getId)
                    .orElse(null);
        } catch (WebApplicationException | ProcessingException e) {
            LOGGER.log(
                    Level.WARNING,
                    "Failed to resolve target pipeline for %s@%s, falling back to default GitLab behavior: %s"
                            .formatted(projectId, sha, e.getMessage()));
            return null;
        }
    }

    private static boolean existsCommit(GitLabClient client, String gitlabProjectId, String commitHash) {
        try {
            client.getCommit(gitlabProjectId, commitHash);
            return true;
        } catch (NotFoundException e) {
            LOGGER.log(
                    Level.FINE,
                    "Project (%s) and commit (%s) combination not found".formatted(gitlabProjectId, commitHash));
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
            addGitLabBranchBuild(
                    result, getBuildRevision(build), buildDatas.get(0).getRemoteUrls(), environment, gitLabClient);
        } else {
            final SCMRevisionAction scmRevisionAction = build.getAction(SCMRevisionAction.class);

            if (scmRevisionAction == null) {
                LOGGER.log(Level.INFO, "Build does not contain SCM revision action.");
                return result;
            }

            final SCMRevision scmRevision = scmRevisionAction.getRevision();

            String scmRevisionHash = null;
            if (scmRevision instanceof AbstractGitSCMSource.SCMRevisionImpl impl) {
                if (scmRevision == null) {
                    LOGGER.log(Level.INFO, "Build does not contain SCM revision object.");
                    return result;
                }
                scmRevisionHash = impl.getHash();
                if (scmRevisionHash == null) {
                    LOGGER.log(Level.INFO, "Build does not contain SCM revision hash.");
                    return result;
                }

                for (final BuildData buildData : buildDatas) {
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
            GitLabClient gitLabClient) {
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
                                    .getProject(projectNameWithNameSpace)
                                    .getId()
                                    .toString();
                        } catch (WebApplicationException | ProcessingException e) {
                            LOGGER.log(
                                    Level.SEVERE,
                                    "Failed to retrieve projectId for project '%s'".formatted(projectNameWithNameSpace),
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
            if (cause instanceof UpstreamCause upstreamCause) {
                List<Cause> upCauses =
                        upstreamCause.getUpstreamCauses(); // Non null, returns empty list when none are set
                for (Cause upCause : upCauses) {
                    if (upCause instanceof GitLabWebHookCause gitlabCause) {
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
