package com.dabsquared.gitlabjenkins.util;

import java.util.List;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Awardable;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Branch;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Group;
import com.dabsquared.gitlabjenkins.gitlab.api.model.GroupOrderType;
import com.dabsquared.gitlabjenkins.gitlab.api.model.GroupSortType;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Label;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Pipeline;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.api.model.ProjectHook;
import com.dabsquared.gitlabjenkins.gitlab.api.model.User;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;

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
    public List<Group> getGroups() {
    	return null;
    }
    
    @Override
    public List<Group> getGroups(Boolean allAvailable, Boolean topLevelOnly, GroupOrderType orderBy,
    		GroupSortType sort) {
    	return null;
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
    public List<ProjectHook> getProjectHooks(String projectName) {
    	return null;
    }

    @Override
    public void addProjectHook(String projectId, String url, String secretToken, Boolean pushEvents, Boolean mergeRequestEvents, Boolean noteEvents) {

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
    public void acceptMergeRequest(MergeRequest mr, String mergeCommitMessage, Boolean shouldRemoveSourceBranch) {

    }

    @Override
    public void createMergeRequestNote(MergeRequest mr, String body) {

    }

    @Override
    public List<Awardable> getMergeRequestEmoji(MergeRequest mr) {
        return null;
    }

    @Override
    public void awardMergeRequestEmoji(MergeRequest mr, String name) {

    }

    @Override
    public void deleteMergeRequestEmoji(MergeRequest mr, Integer awardId) {

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
