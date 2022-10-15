package com.dabsquared.gitlabjenkins.gitlab.api.impl;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.*;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import java.util.List;
import java.util.function.Function;

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
    public List<Group> getGroups() {
        return api.getGroups(
            true,
            false,
            OrderType.path.name(),
            SortType.asc.name()
        );
    }

    @Override
    public List<Group> getGroups(Boolean allAvailable, Boolean topLevelOnly, OrderType orderBy, SortType sort) {
        return api.getGroups(
            allAvailable,
            topLevelOnly,
            orderBy == null ? OrderType.path.name() : orderBy.name(),
            sort == null ? SortType.asc.name() : sort.name()
        );
    }

    @Override
    public List<Project> getGroupProjects(String groupId) {
        return api.getGroupProjects(
            groupId,
            Boolean.FALSE,
            null,
            OrderType.path.name(),
            SortType.asc.name()
        );
    }

    @Override
    public List<Project> getGroupProjects(String groupId, Boolean includeSubgroups, ProjectVisibilityType visibility,
            OrderType orderBy, SortType sort) {
        return api.getGroupProjects(
            groupId,
            includeSubgroups == null ? Boolean.FALSE : includeSubgroups,
            visibility == null ? null : visibility.getValue(),
            orderBy == null ? OrderType.path.name() : orderBy.name(),
            sort == null ? SortType.asc.name() : sort.name()
        );
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
    public List<ProjectHook> getProjectHooks(String projectName) {
        return api.getProjectHooks(projectName);
    }

    @Override
    public void addProjectHook(String projectId, String url, Boolean pushEvents, Boolean mergeRequestEvents, Boolean noteEvents) {
        api.addProjectHook(projectId, url, pushEvents, mergeRequestEvents, noteEvents);
    }

    @Override
    public void addProjectHook(String projectId, String url, String secretToken, Boolean pushEvents, Boolean mergeRequestEvents, Boolean noteEvents) {
        api.addProjectHook(projectId, url, secretToken, pushEvents, mergeRequestEvents, noteEvents);
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
    public void acceptMergeRequest(MergeRequest mr, String mergeCommitMessage, Boolean shouldRemoveSourceBranch) {
        api.acceptMergeRequest(mr.getProjectId(), mergeRequestIdProvider.apply(mr), mergeCommitMessage, shouldRemoveSourceBranch);
    }

    @Override
    public void createMergeRequestNote(MergeRequest mr, String body) {
        api.createMergeRequestNote(mr.getProjectId(), mergeRequestIdProvider.apply(mr), body);
    }

    @Override
    public List<Awardable> getMergeRequestEmoji(MergeRequest mr) {
        return api.getMergeRequestEmoji(mr.getProjectId(), mergeRequestIdProvider.apply(mr));
    }

    @Override
    public void awardMergeRequestEmoji(MergeRequest mr, String name) {
        api.awardMergeRequestEmoji(mr.getProjectId(), mergeRequestIdProvider.apply(mr), name);
    }

    @Override
    public void deleteMergeRequestEmoji(MergeRequest mr, Integer awardId) {
        api.deleteMergeRequestEmoji(mr.getProjectId(), mergeRequestIdProvider.apply(mr), awardId);
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
