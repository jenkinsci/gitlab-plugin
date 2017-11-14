package com.dabsquared.gitlabjenkins.service;


import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.*;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;


class GitLabClientStub implements GitLabClient {
    private final Map<Pair<String, Class>, List<?>> data;
    private final Map<Pair<String, Class>, Integer> calls;

    GitLabClientStub() {
        data = new HashMap<>();
        calls = new HashMap<>();
    }

    @Override
    public String getHostUrl() {
        return "";
    }

    void addBranches(String project, List<Branch> branches) {
        addData(project, Branch.class, branches);
    }

    void addLabels(String project, List<Label> labels) {
        addData(project, Label.class, labels);
    }

    int calls(String projectId, Class dataClass) {
        Pair<String, Class> key = createKey(projectId, dataClass);
        return calls.containsKey(key) ? calls.get(key) : 0;
    }

    @Override
    public List<Branch> getBranches(String projectId) {
        return getData(projectId, Branch.class);
    }

    @Override
    public List<Label> getLabels(String projectId) {
        return getData(projectId, Label.class);
    }

    private void addData(String projectId, Class dataClass, List<?> datas) {
        data.put(createKey(projectId, dataClass), datas);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getData(String projectId, Class dataClass) {
        Pair<String, Class> key = createKey(projectId, dataClass);
        if (!calls.containsKey(key)) {
            calls.put(key, 0);
        }

        calls.put(key, calls.get(key) + 1);

        return (List<T>) data.get(key);
    }

    private Pair<String, Class> createKey(String projectId, Class dataClass) {
        return new ImmutablePair<>(projectId, dataClass);
    }


    /************** no implementation below ********************************/

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
    public List<Pipeline> getPipelines(String projectName) {
        return emptyList();
    }
}
