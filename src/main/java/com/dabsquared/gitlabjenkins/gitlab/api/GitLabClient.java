package com.dabsquared.gitlabjenkins.gitlab.api;


import com.dabsquared.gitlabjenkins.gitlab.api.model.*;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.util.List;


public final class GitLabClient implements GitLabApi {
    private final String hostUrl;
    private final GitLabApi api;

    @Restricted(NoExternalUse.class)
    public GitLabClient(String hostUrl, GitLabApi api) {
        this.hostUrl = hostUrl;
        this.api = api;
    }

    public final String getHostUrl() {
        return hostUrl;
    }

    @Override
    public Project createProject(String projectName) {
        return api.createProject(projectName);
    }

    @Override
    public MergeRequest createMergeRequest(Integer projectId, String sourceBranch, String targetBranch, String title) {
        return api.createMergeRequest(projectId, sourceBranch, targetBranch, title);
    }

    @Override
    public Project getProject(String projectName) {
        return api.getProject(projectName);
    }

    @Override
    public Project updateProject(String projectId, String name, String path) {
        return api.updateProject(projectId, name, path);
    }

    @Override
    public void deleteProject(String projectId) {
        api.deleteProject(projectId);
    }

    @Override
    public void addProjectHook(String projectId, String url, Boolean pushEvents, Boolean mergeRequestEvents, Boolean noteEvents) {
        api.addProjectHook(projectId, url, pushEvents, mergeRequestEvents, noteEvents);
    }

    @Override
    public void changeBuildStatus(String projectId, String sha, BuildState state, String ref, String context, String targetUrl, String description) {
        api.changeBuildStatus(projectId, sha, state, ref, context, targetUrl, description);
    }

    @Override
    public void changeBuildStatus(Integer projectId, String sha, BuildState state, String ref, String context, String targetUrl, String description) {
        api.changeBuildStatus(projectId, sha, state, ref, context, targetUrl, description);
    }

    @Override
    public void getCommit(String projectId, String sha) {
        api.getCommit(projectId, sha);
    }

    @Override
    public void acceptMergeRequest(Integer projectId, Integer mergeRequestId, String mergeCommitMessage, boolean shouldRemoveSourceBranch) {
        api.acceptMergeRequest(projectId, mergeRequestId, mergeCommitMessage, shouldRemoveSourceBranch);
    }

    @Override
    public void createMergeRequestNote(Integer projectId, Integer mergeRequestId, String body) {
        api.createMergeRequestNote(projectId, mergeRequestId, body);
    }

    @Override
    public List<MergeRequest> getMergeRequests(String projectId, State state, int page, int perPage) {
        return api.getMergeRequests(projectId, state, page, perPage);
    }

    @Override
    public List<Branch> getBranches(String projectId) {
        return api.getBranches(projectId);
    }

    @Override
    public Branch getBranch(String projectId, String branch) {
        return api.getBranch(projectId, branch);
    }

    @Override
    public void headCurrentUser() {
        api.headCurrentUser();
    }

    @Override
    public User getCurrentUser() {
        return api.getCurrentUser();
    }

    @Override
    public User addUser(String email, String username, String name, String password) {
        return api.addUser(email, username, name, password);
    }

    @Override
    public User updateUser(String userId, String email, String username, String name, String password) {
        return api.updateUser(userId, email, username, name, password);
    }

    @Override
    public List<Label> getLabels(String projectId) {
        return api.getLabels(projectId);
    }

    @Override
    public List<Pipeline> getPipelines(String projectName) {
        return api.getPipelines(projectName);
    }
}
