package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Branch;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Label;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Pipeline;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.api.model.User;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;

import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.NoSuchElementException;


final class AutodetectingGitlabApi implements GitLabApi {
    private final Iterable<GitLabClientBuilder> builders;
    private final String url;
    private final String token;
    private final boolean ignoreCertificateErrors;
    private final int connectionTimeout;
    private final int readTimeout;
    private GitLabApi delegate;

    
    AutodetectingGitlabApi(Iterable<GitLabClientBuilder> builders, String url, String token, boolean ignoreCertificateErrors, int connectionTimeout, int readTimeout) {
        this.builders = builders;
        this.url = url;
        this.token = token;
        this.ignoreCertificateErrors = ignoreCertificateErrors;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }

    @Override
    public Project createProject(final String projectName) {
        return new GitLabOperation<Project>() {
            @Override
            Project execute(GitLabApi api) {
                return api.createProject(projectName);
            }
        }.execute();
    }

    @Override
    public MergeRequest createMergeRequest(final Integer projectId, final String sourceBranch, final String targetBranch, final String title) {
        return new GitLabOperation<MergeRequest>() {
            @Override
            MergeRequest execute(GitLabApi api) {
                return api.createMergeRequest(projectId, sourceBranch, targetBranch, title);
            }
        }.execute();
    }

    @Override
    public Project getProject(final String projectName) {
        return new GitLabOperation<Project>() {
            @Override
            Project execute(GitLabApi api) {
                return api.getProject(projectName);
            }
        }.execute();
    }

    @Override
    public Project updateProject(final String projectId, final String name, final String path) {
        return new GitLabOperation<Project>() {
            @Override
            Project execute(GitLabApi api) {
                return api.updateProject(projectId, name, path);
            }
        }.execute();
    }

    @Override
    public void deleteProject(final String projectId) {
        new GitLabOperation<Void>() {
            @Override
            Void execute(GitLabApi api) {
                api.deleteProject(projectId);
                return null;
            }
        }.execute();
    }

    @Override
    public void addProjectHook(final String projectId, final String url, final Boolean pushEvents, final Boolean mergeRequestEvents, final Boolean noteEvents) {
        new GitLabOperation<Void>() {
            @Override
            Void execute(GitLabApi api) {
                api.addProjectHook(projectId, url, pushEvents, mergeRequestEvents, noteEvents);
                return null;
            }
        }.execute();
    }

    @Override
    public void changeBuildStatus(final String projectId, final String sha, final BuildState state, final String ref, final String context, final String targetUrl, final String description) {
        new GitLabOperation<Void>() {
            @Override
            Void execute(GitLabApi api) {
                api.changeBuildStatus(projectId, sha, state, ref, context, targetUrl, description);
                return null;
            }
        }.execute();
    }

    @Override
    public void changeBuildStatus(final Integer projectId, final String sha, final BuildState state, final String ref, final String context, final String targetUrl, final String description) {
        new GitLabOperation<Void>() {
            @Override
            Void execute(GitLabApi api) {
                api.changeBuildStatus(projectId, sha, state, ref, context, targetUrl, description);
                return null;
            }
        }.execute();
    }

    @Override
    public void getCommit(final String projectId, final String sha) {
        new GitLabOperation<Void>() {
            @Override
            Void execute(GitLabApi api) {
                api.getCommit(projectId, sha);
                return null;
            }
        }.execute();
    }

    @Override
    public void acceptMergeRequest(final Integer projectId, final Integer mergeRequestId, final String mergeCommitMessage, final boolean shouldRemoveSourceBranch) {
        new GitLabOperation<Void>() {
            @Override
            Void execute(GitLabApi api) {
                api.acceptMergeRequest(projectId, mergeRequestId, mergeCommitMessage, shouldRemoveSourceBranch);
                return null;
            }
        }.execute();
    }

    @Override
    public void createMergeRequestNote(final Integer projectId, final Integer mergeRequestId, final String body) {
        new GitLabOperation<Void>() {
            @Override
            Void execute(GitLabApi api) {
                api.createMergeRequestNote(projectId, mergeRequestId, body);
                return null;
            }
        }.execute();
    }

    @Override
    public List<MergeRequest> getMergeRequests(final String projectId, final State state, final int page, final int perPage) {
        return new GitLabOperation<List<MergeRequest>>() {
            @Override
            List<MergeRequest> execute(GitLabApi api) {
                return api.getMergeRequests(projectId, state, page, perPage);
            }
        }.execute();
    }

    @Override
    public List<Branch> getBranches(final String projectId) {
        return new GitLabOperation<List<Branch>>() {
            @Override
            List<Branch> execute(GitLabApi api) {
                return api.getBranches(projectId);
            }
        }.execute();
    }

    @Override
    public Branch getBranch(final String projectId, final String branch) {
        return new GitLabOperation<Branch>() {
            @Override
            Branch execute(GitLabApi api) {
                return api.getBranch(projectId, branch);
            }
        }.execute();
    }

    @Override
    public void headCurrentUser() {
        new GitLabOperation<Void>() {
            @Override
            Void execute(GitLabApi api) {
                api.headCurrentUser();
                return null;
            }
        }.execute();
    }

    @Override
    public User getCurrentUser() {
        return new GitLabOperation<User>() {
            @Override
            User execute(GitLabApi api) {
                return api.getCurrentUser();
            }
        }.execute();
    }

    @Override
    public User addUser(final String email, final String username, final String name, final String password) {
        return new GitLabOperation<User>() {
            @Override
            User execute(GitLabApi api) {
                return api.addUser(email, username, name, password);
            }
        }.execute();
    }

    @Override
    public User updateUser(final String userId, final String email, final String username, final String name, final String password) {
        return new GitLabOperation<User>() {
            @Override
            User execute(GitLabApi api) {
                return api.updateUser(userId, email, username, name, password);
            }
        }.execute();
    }

    @Override
    public List<Label> getLabels(final String projectId) {
        return new GitLabOperation<List<Label>>() {
            @Override
            List<Label> execute(GitLabApi api) {
                return api.getLabels(projectId);
            }
        }.execute();
    }

    @Override
    public List<Pipeline> getPipelines(final String projectName) {
        return new GitLabOperation<List<Pipeline>>() {
            @Override
            List<Pipeline> execute(GitLabApi api) {
                return api.getPipelines(projectName);
            }
        }.execute();
    }


    private GitLabApi delegate(boolean reset) {
        if (reset || delegate == null) {
            delegate = autodetectOrDie();
        }

        return delegate;
    }

    private GitLabClient autodetectOrDie() {
        GitLabClient client = autodetect();
        if (client != null) {
            return client;
        }

        throw new NoSuchElementException("no client-builder found that supports server at " + url);
    }

    private GitLabClient autodetect() {
        for (GitLabClientBuilder candidate : builders) {
            GitLabClient client = candidate.buildClient(url, token, ignoreCertificateErrors, connectionTimeout, readTimeout);
            try {
                client.headCurrentUser();
                return client;
            } catch (NotFoundException ignored) {
                // api-endpoint not found (== api-level not supported by this client)
            }
        }

        return null;
    }

    
    private abstract class GitLabOperation<R> {
        final R execute() {
            return execute(false);
        }

        private R execute(boolean reset) {
            try {
                return execute(delegate(reset));
            } catch (NotFoundException e) {
                if (reset) {
                    throw e;
                }

                return execute(true);
            }
        }


        abstract R execute(GitLabApi api);
    }
}
