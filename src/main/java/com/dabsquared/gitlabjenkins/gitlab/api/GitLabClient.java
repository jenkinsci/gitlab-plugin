package com.dabsquared.gitlabjenkins.gitlab.api;

import com.dabsquared.gitlabjenkins.gitlab.api.model.*;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import java.util.List;

public interface GitLabClient {
    String getHostUrl();

    List<Group> getGroups();

    List<Project> getGroupProjects(String groupId);

    List<Project> getGroupProjects(
            String groupId,
            Boolean includeSubgroups,
            ProjectVisibilityType visibility,
            OrderType orderBy,
            SortType sort);

    List<Group> getGroups(Boolean allAvailable, Boolean topLevelOnly, OrderType orderBy, SortType sort);

    Project createProject(String projectName);

    MergeRequest createMergeRequest(Integer projectId, String sourceBranch, String targetBranch, String title);

    Project getProject(String projectName);

    Project updateProject(String projectId, String name, String path);

    void deleteProject(String projectId);

    List<ProjectHook> getProjectHooks(String projectName);

    void addProjectHook(
            String projectId, String url, Boolean pushEvents, Boolean mergeRequestEvents, Boolean noteEvents);

    void addProjectHook(
            String projectId,
            String url,
            String secretToken,
            Boolean pushEvents,
            Boolean mergeRequestEvents,
            Boolean noteEvents);

    void changeBuildStatus(
            String projectId,
            String sha,
            BuildState state,
            String ref,
            String context,
            String targetUrl,
            String description);

    void changeBuildStatus(
            Integer projectId,
            String sha,
            BuildState state,
            String ref,
            String context,
            String targetUrl,
            String description);

    /**
     * Same as the four-arg-tail overloads above, but lets the caller pin the status to a
     * specific pipeline. Without this, GitLab decides which pipeline to attach the status
     * to (or spins up a throwaway "external" one) based on sha+ref+context alone, which can
     * silently disagree with whichever pipeline is actually the MR's head_pipeline. Pass
     * null to fall back to the old, GitLab-decides behavior.
     *
     * Default implementation just ignores pipelineId and falls back to the old behavior,
     * so existing GitLabClient implementations (e.g. test stubs) keep compiling unchanged.
     */
    default void changeBuildStatus(
            String projectId,
            String sha,
            BuildState state,
            String ref,
            String context,
            String targetUrl,
            String description,
            Integer pipelineId) {
        changeBuildStatus(projectId, sha, state, ref, context, targetUrl, description);
    }

    default void changeBuildStatus(
            Integer projectId,
            String sha,
            BuildState state,
            String ref,
            String context,
            String targetUrl,
            String description,
            Integer pipelineId) {
        changeBuildStatus(projectId, sha, state, ref, context, targetUrl, description);
    }

    void getCommit(String projectId, String sha);

    void acceptMergeRequest(MergeRequest mr, String mergeCommitMessage, Boolean shouldRemoveSourceBranch);

    void createMergeRequestNote(MergeRequest mr, String body);

    List<Awardable> getMergeRequestEmoji(MergeRequest mr);

    void awardMergeRequestEmoji(MergeRequest mr, String name);

    void deleteMergeRequestEmoji(MergeRequest mr, Integer awardId);

    List<MergeRequest> getMergeRequests(String projectId, State state, int page, int perPage);

    List<Branch> getBranches(String projectId);

    Branch getBranch(String projectId, String branch);

    User getCurrentUser();

    User addUser(String email, String username, String name, String password);

    User updateUser(String userId, String email, String username, String name, String password);

    List<Label> getLabels(String projectId);

    List<Pipeline> getPipelines(String projectName);

    /**
     * Same as {@link #getPipelines(String)} but filtered to a single commit SHA.
     * Default implementation falls back to fetching all pipelines unfiltered, so existing
     * GitLabClient implementations (e.g. test stubs) keep compiling unchanged.
     */
    default List<Pipeline> getPipelines(String projectName, String sha) {
        return getPipelines(projectName);
    }

    List<MergeRequest> getCommitMergeRequests(String projectId, String sha);
}
