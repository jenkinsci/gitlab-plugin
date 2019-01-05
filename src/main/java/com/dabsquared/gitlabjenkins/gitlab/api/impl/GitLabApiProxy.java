package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import com.dabsquared.gitlabjenkins.gitlab.api.model.*;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;

import javax.ws.rs.core.Response;
import java.util.List;


interface GitLabApiProxy {
    Project createProject(String projectName);

    MergeRequest createMergeRequest(Integer projectId, String sourceBranch, String targetBranch, String title);

    Project getProject(String projectName);

    Project updateProject(String projectId, String name, String path);

    void deleteProject(String projectId);

    void addProjectHook(String projectId, String url, Boolean pushEvents, Boolean mergeRequestEvents, Boolean noteEvents);

    void changeBuildStatus(String projectId, String sha, BuildState state, String ref, String context, String targetUrl, String description);

    void changeBuildStatus(Integer projectId, String sha, BuildState state, String ref, String context, String targetUrl, String description);

    void getCommit(String projectId, String sha);

    void acceptMergeRequest(Integer projectId, Integer mergeRequestId, String mergeCommitMessage, boolean shouldRemoveSourceBranch);

    void createMergeRequestNote(Integer projectId, Integer mergeRequestId, String body);

    List<Awardable> getMergeRequestEmoji(Integer projectId, Integer mergeRequestId);

    void awardMergeRequestEmoji(Integer projectId, Integer mergeRequestId, String name);

    void deleteMergeRequestEmoji(Integer projectId, Integer mergeRequestId, Integer awardId);

    List<MergeRequest> getMergeRequests(String projectId, State state, int page, int perPage);

    List<Branch> getBranches(String projectId);

    Branch getBranch(String projectId, String branch);

    void headCurrentUser();

    User getCurrentUser();

    User addUser(String email, String username, String name, String password);

    User updateUser(String userId, String email, String username, String name, String password);

    Response getLabels(String projectId, int page);

    List<Pipeline> getPipelines(String projectName);
}
