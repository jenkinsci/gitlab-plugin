package com.dabsquared.gitlabjenkins.gitlab.api;

import com.dabsquared.gitlabjenkins.gitlab.api.model.*;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;

import java.util.List;

public interface GitLabClient {
    String getHostUrl();

    Project createProject(String projectName);

    MergeRequest createMergeRequest(Integer projectId, String sourceBranch, String targetBranch, String title);

    Project getProject(String projectName);

    Project updateProject(String projectId, String name, String path);

    void deleteProject(String projectId);

    void addProjectHook(String projectId, String url, Boolean pushEvents, Boolean mergeRequestEvents, Boolean noteEvents);

    void changeBuildStatus(String projectId, String sha, BuildState state, String ref, String context, String targetUrl, String description);

    void changeBuildStatus(Integer projectId, String sha, BuildState state, String ref, String context, String targetUrl, String description);

    void getCommit(String projectId, String sha);

    void acceptMergeRequest(MergeRequest mr, String mergeCommitMessage, boolean shouldRemoveSourceBranch);

    void createMergeRequestNote(MergeRequest mr, String body);

    List<MergeRequest> getMergeRequests(String projectId, State state, int page, int perPage);

    List<Branch> getBranches(String projectId);

    Branch getBranch(String projectId, String branch);

    void headCurrentUser();

    User getCurrentUser();

    User addUser(String email, String username, String name, String password);

    User updateUser(String userId, String email, String username, String name, String password);

    List<Label> getLabels(String projectId);

    List<Pipeline> getPipelines(String projectName);
}
