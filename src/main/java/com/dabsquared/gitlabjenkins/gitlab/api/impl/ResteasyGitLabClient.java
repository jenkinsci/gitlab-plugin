package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.*;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.google.common.base.Function;

import java.util.List;


final class ResteasyGitLabClient implements GitLabClient {
    private final String hostUrl;
    private final GitLabApiProxy api;
    private final Function<MergeRequest, Integer> mergeRequestIdProvider;

    ResteasyGitLabClient(String hostUrl, GitLabApiProxy api, Function<MergeRequest, Integer> mergeRequestIdProvider) {
        this.hostUrl = hostUrl;
        this.api = api;
        this.mergeRequestIdProvider = mergeRequestIdProvider;
    }

    @Override
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
    public void acceptMergeRequest(MergeRequest mr, String mergeCommitMessage, boolean shouldRemoveSourceBranch) {
        api.acceptMergeRequest(mr.getProjectId(), mergeRequestIdProvider.apply(mr), mergeCommitMessage, shouldRemoveSourceBranch);
    }

    @Override
    public void createMergeRequestNote(MergeRequest mr, String body) {
        api.createMergeRequestNote(mr.getProjectId(), mergeRequestIdProvider.apply(mr), body);
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
