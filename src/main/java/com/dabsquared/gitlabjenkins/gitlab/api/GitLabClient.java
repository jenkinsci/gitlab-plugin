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
}
