package com.dabsquared.gitlabjenkins.util;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.*;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;

import java.util.List;

class GitLabClientStub implements GitLabClient {
    private final String url;

    GitLabClientStub(String url) {
        this.url = url;
    }

    @Override
    public String getHostUrl() {
        return url;
    }

    @Override
    public Project createProject(String projectName) {
        return null;
    }

    @Override
    public MergeRequest createMergeRequest(Integer projectId, String sourceBranch, String targetBranch, String title) {
        return null;
    }

    @Override
    public Project getProject(String projectName) {
        return null;
    }

    @Override
    public Project updateProject(String projectId, String name, String path) {
        return null;
    }

    @Override
    public void deleteProject(String projectId) {

    }

    @Override
    public void addProjectHook(String projectId, String url, Boolean pushEvents, Boolean mergeRequestEvents, Boolean noteEvents) {

    }

    @Override
    public void changeBuildStatus(String projectId, String sha, BuildState state, String ref, String context, String targetUrl, String description) {

    }

    @Override
    public void changeBuildStatus(Integer projectId, String sha, BuildState state, String ref, String context, String targetUrl, String description) {

    }

    @Override
    public void getCommit(String projectId, String sha) {

    }

    @Override
    public void acceptMergeRequest(MergeRequest mr, String mergeCommitMessage, boolean shouldRemoveSourceBranch) {

    }

    @Override
    public void createMergeRequestNote(MergeRequest mr, String body) {

    }

    @Override
    public List<MergeRequest> getMergeRequests(String projectId, State state, int page, int perPage) {
        return null;
    }

    @Override
    public List<Branch> getBranches(String projectId) {
        return null;
    }

    @Override
    public Branch getBranch(String projectId, String branch) {
        return null;
    }

    @Override
    public void headCurrentUser() {

    }

    @Override
    public User getCurrentUser() {
        return null;
    }

    @Override
    public User addUser(String email, String username, String name, String password) {
        return null;
    }

    @Override
    public User updateUser(String userId, String email, String username, String name, String password) {
        return null;
    }

    @Override
    public List<Label> getLabels(String projectId) {
        return null;
    }

    @Override
    public List<Pipeline> getPipelines(String projectName) {
        return null;
    }
}
