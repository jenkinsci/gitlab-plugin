package com.dabsquared.gitlabjenkins.gitlab;

import java.util.List;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApiClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Branch;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Label;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.api.model.User;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;

/**
 * Implements {@link GitLabApi} by delegating REST-client calls to RESTEasy proxy.
 * 
 * @author Alexander Leshkin
 *
 */
class GitLabApiExtension implements GitLabApi {

    private GitLabApiClient client;
    private String gitlabHostUrl;

    public GitLabApiExtension(GitLabApiClient client, String gitlabHostUrl) {
        this.client = client;
        this.gitlabHostUrl = gitlabHostUrl;
    }

    @Override
    public String getGitLabHostUrl() {
        return gitlabHostUrl;
    }

    @Override
    public Project createProject(String projectName) {
        return client.createProject(projectName);
    }

    @Override
    public MergeRequest createMergeRequest(Integer projectId, String sourceBranch, String targetBranch, String title) {
        return client.createMergeRequest(projectId, sourceBranch, targetBranch, title);
    }

    @Override
    public Project getProject(String projectName) {
        return client.getProject(projectName);
    }

    @Override
    public Project updateProject(String projectId, String name, String path) {
        return client.updateProject(projectId, name, path);
    }

    @Override
    public void deleteProject(String projectId) {
        client.deleteProject(projectId);
    }

    @Override
    public void addProjectHook(String projectId, String url, Boolean pushEvents, Boolean mergeRequestEvents,
            Boolean noteEvents) {
        client.addProjectHook(projectId, url, pushEvents, mergeRequestEvents, noteEvents);
    }

    @Override
    public void changeBuildStatus(String projectId, String sha, BuildState state, String ref, String context,
            String targetUrl, String description) {
        client.changeBuildStatus(projectId, sha, state, ref, context, targetUrl, description);
    }

    @Override
    public void changeBuildStatus(Integer projectId, String sha, BuildState state, String ref, String context,
            String targetUrl, String description) {
        client.changeBuildStatus(projectId, sha, state, ref, context, targetUrl, description);
    }

    @Override
    public void getCommit(String projectId, String sha) {
        client.getCommit(projectId, sha);
    }

    @Override
    public void acceptMergeRequest(Integer projectId, Integer mergeRequestId, String mergeCommitMessage,
            boolean shouldRemoveSourceBranch) {
        client.acceptMergeRequest(projectId, mergeRequestId, mergeCommitMessage, shouldRemoveSourceBranch);
    }

    @Override
    public void createMergeRequestNote(Integer projectId, Integer mergeRequestId, String body) {
        client.createMergeRequestNote(projectId, mergeRequestId, body);
    }

    @Override
    public List<MergeRequest> getMergeRequests(String projectId, State state, int page, int perPage) {
        return client.getMergeRequests(projectId, state, page, perPage);
    }

    @Override
    public List<Branch> getBranches(String projectId) {
        return client.getBranches(projectId);
    }

    @Override
    public Branch getBranch(String projectId, String branch) {
        return client.getBranch(projectId, branch);
    }

    @Override
    public void headCurrentUser() {
        client.headCurrentUser();
    }

    @Override
    public User getCurrentUser() {
        return client.getCurrentUser();
    }

    @Override
    public User addUser(String email, String username, String name, String password) {
        return client.addUser(email, username, name, password);
    }

    @Override
    public User updateUser(String userId, String email, String username, String name, String password) {
        return client.updateUser(userId, email, username, name, password);
    }

    @Override
    public List<Label> getLabels(String projectId) {
        return client.getLabels(projectId);
    }
}
