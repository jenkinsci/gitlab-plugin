package com.dabsquared.gitlabjenkins.gitlab.api.impl;

import com.dabsquared.gitlabjenkins.connection.GitlabCredentialResolver;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import com.dabsquared.gitlabjenkins.gitlab.api.model.*;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import java.util.NoSuchElementException;

final class AutodetectingGitLabClient implements GitLabClient {
    private final Iterable<GitLabClientBuilder> builders;
    private final String url;
    private final GitlabCredentialResolver credentialResolver;
    private final boolean ignoreCertificateErrors;
    private final int connectionTimeout;
    private final int readTimeout;
    private GitLabClient delegate;

    AutodetectingGitLabClient(
            Iterable<GitLabClientBuilder> builders,
            String url,
            GitlabCredentialResolver credentialResolver,
            boolean ignoreCertificateErrors,
            int connectionTimeout,
            int readTimeout) {
        this.builders = builders;
        this.url = url;
        this.credentialResolver = credentialResolver;
        this.ignoreCertificateErrors = ignoreCertificateErrors;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }

    @Override
    public String getHostUrl() {
        return url;
    }

    @Override
    public List<Group> getGroups() {
        return execute(new GitLabOperation<List<Group>>() {
            @Override
            List<Group> execute(GitLabClient client) {
                return client.getGroups();
            }
        });
    }

    @Override
    public List<Project> getGroupProjects(String groupId) {
        return execute(new GitLabOperation<List<Project>>() {
            @Override
            List<Project> execute(GitLabClient client) {
                return client.getGroupProjects(groupId);
            }
        });
    }

    @Override
    public List<Project> getGroupProjects(
            String groupId,
            Boolean includeSubgroups,
            ProjectVisibilityType visibility,
            OrderType orderBy,
            SortType sort) {
        return execute(new GitLabOperation<List<Project>>() {
            @Override
            List<Project> execute(GitLabClient client) {
                return client.getGroupProjects(groupId, includeSubgroups, visibility, orderBy, sort);
            }
        });
    }

    @Override
    public List<Group> getGroups(Boolean allAvailable, Boolean topLevelOnly, OrderType orderBy, SortType sort) {
        return execute(new GitLabOperation<List<Group>>() {
            @Override
            List<Group> execute(GitLabClient client) {
                return client.getGroups(allAvailable, topLevelOnly, orderBy, sort);
            }
        });
    }

    @Override
    public Project createProject(final String projectName) {
        return execute(new GitLabOperation<Project>() {
            @Override
            Project execute(GitLabClient client) {
                return client.createProject(projectName);
            }
        });
    }

    @Override
    public MergeRequest createMergeRequest(
            final Integer projectId, final String sourceBranch, final String targetBranch, final String title) {
        return execute(new GitLabOperation<MergeRequest>() {
            @Override
            MergeRequest execute(GitLabClient client) {
                return client.createMergeRequest(projectId, sourceBranch, targetBranch, title);
            }
        });
    }

    @Override
    public Project getProject(final String projectName) {
        return execute(new GitLabOperation<Project>() {
            @Override
            Project execute(GitLabClient client) {
                return client.getProject(projectName);
            }
        });
    }

    @Override
    public List<ProjectHook> getProjectHooks(String projectName) {
        return execute(new GitLabOperation<List<ProjectHook>>() {
            @Override
            List<ProjectHook> execute(GitLabClient client) {
                return client.getProjectHooks(projectName);
            }
        });
    }

    @Override
    public Project updateProject(final String projectId, final String name, final String path) {
        return execute(new GitLabOperation<Project>() {
            @Override
            Project execute(GitLabClient client) {
                return client.updateProject(projectId, name, path);
            }
        });
    }

    @Override
    public void deleteProject(final String projectId) {
        execute(new GitLabOperation<Void>() {
            @Override
            Void execute(GitLabClient client) {
                client.deleteProject(projectId);
                return null;
            }
        });
    }

    @Override
    public void addProjectHook(
            final String projectId,
            final String url,
            final Boolean pushEvents,
            final Boolean mergeRequestEvents,
            final Boolean noteEvents) {
        execute(new GitLabOperation<Void>() {
            @Override
            Void execute(GitLabClient client) {
                client.addProjectHook(projectId, url, pushEvents, mergeRequestEvents, noteEvents);
                return null;
            }
        });
    }

    @Override
    public void addProjectHook(
            final String projectId,
            final String url,
            String secretToken,
            final Boolean pushEvents,
            final Boolean mergeRequestEvents,
            final Boolean noteEvents) {
        execute(new GitLabOperation<Void>() {
            @Override
            Void execute(GitLabClient client) {
                client.addProjectHook(projectId, url, secretToken, pushEvents, mergeRequestEvents, noteEvents);
                return null;
            }
        });
    }

    @Override
    public void changeBuildStatus(
            final String projectId,
            final String sha,
            final BuildState state,
            final String ref,
            final String context,
            final String targetUrl,
            final String description) {
        execute(new GitLabOperation<Void>() {
            @Override
            Void execute(GitLabClient client) {
                client.changeBuildStatus(projectId, sha, state, ref, context, targetUrl, description);
                return null;
            }
        });
    }

    @Override
    public void changeBuildStatus(
            final Integer projectId,
            final String sha,
            final BuildState state,
            final String ref,
            final String context,
            final String targetUrl,
            final String description) {
        execute(new GitLabOperation<Void>() {
            @Override
            Void execute(GitLabClient client) {
                client.changeBuildStatus(projectId, sha, state, ref, context, targetUrl, description);
                return null;
            }
        });
    }

    @Override
    public void getCommit(final String projectId, final String sha) {
        execute(new GitLabOperation<Void>() {
            @Override
            Void execute(GitLabClient client) {
                client.getCommit(projectId, sha);
                return null;
            }
        });
    }

    @Override
    public void acceptMergeRequest(
            final MergeRequest mr, final String mergeCommitMessage, final Boolean shouldRemoveSourceBranch) {
        execute(new GitLabOperation<Void>() {
            @Override
            Void execute(GitLabClient client) {
                client.acceptMergeRequest(mr, mergeCommitMessage, shouldRemoveSourceBranch);
                return null;
            }
        });
    }

    @Override
    public void createMergeRequestNote(final MergeRequest mr, final String body) {
        execute(new GitLabOperation<Void>() {
            @Override
            Void execute(GitLabClient client) {
                client.createMergeRequestNote(mr, body);
                return null;
            }
        });
    }

    @Override
    public List<Awardable> getMergeRequestEmoji(final MergeRequest mr) {
        return execute(new GitLabOperation<List<Awardable>>() {
            @Override
            List<Awardable> execute(GitLabClient client) {
                return client.getMergeRequestEmoji(mr);
            }
        });
    }

    @Override
    public void awardMergeRequestEmoji(final MergeRequest mr, final String body) {
        execute(new GitLabOperation<Void>() {
            @Override
            Void execute(GitLabClient client) {
                client.awardMergeRequestEmoji(mr, body);
                return null;
            }
        });
    }

    @Override
    public void deleteMergeRequestEmoji(final MergeRequest mr, final Integer awardId) {
        execute(new GitLabOperation<Void>() {
            @Override
            Void execute(GitLabClient client) {
                client.deleteMergeRequestEmoji(mr, awardId);
                return null;
            }
        });
    }

    @Override
    public List<MergeRequest> getMergeRequests(
            final String projectId, final State state, final int page, final int perPage) {
        return execute(new GitLabOperation<List<MergeRequest>>() {
            @Override
            List<MergeRequest> execute(GitLabClient client) {
                return client.getMergeRequests(projectId, state, page, perPage);
            }
        });
    }

    @Override
    public List<Branch> getBranches(final String projectId) {
        return execute(new GitLabOperation<List<Branch>>() {
            @Override
            List<Branch> execute(GitLabClient client) {
                return client.getBranches(projectId);
            }
        });
    }

    @Override
    public Branch getBranch(final String projectId, final String branch) {
        return execute(new GitLabOperation<Branch>() {
            @Override
            Branch execute(GitLabClient client) {
                return client.getBranch(projectId, branch);
            }
        });
    }

    @Override
    public User getCurrentUser() {
        return execute(new GitLabOperation<User>() {
            @Override
            User execute(GitLabClient client) {
                return client.getCurrentUser();
            }
        });
    }

    @Override
    public User addUser(final String email, final String username, final String name, final String password) {
        return execute(new GitLabOperation<User>() {
            @Override
            User execute(GitLabClient client) {
                return client.addUser(email, username, name, password);
            }
        });
    }

    @Override
    public User updateUser(
            final String userId, final String email, final String username, final String name, final String password) {
        return execute(new GitLabOperation<User>() {
            @Override
            User execute(GitLabClient client) {
                return client.updateUser(userId, email, username, name, password);
            }
        });
    }

    @Override
    public List<Label> getLabels(final String projectId) {
        return execute(new GitLabOperation<List<Label>>() {
            @Override
            List<Label> execute(GitLabClient client) {
                return client.getLabels(projectId);
            }
        });
    }

    @Override
    public List<Pipeline> getPipelines(final String projectName) {
        return execute(new GitLabOperation<List<Pipeline>>() {
            @Override
            List<Pipeline> execute(GitLabClient client) {
                return client.getPipelines(projectName);
            }
        });
    }

    private GitLabClient delegate(boolean reset) {
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
            GitLabClient client = candidate.buildClient(
                    url, credentialResolver, ignoreCertificateErrors, connectionTimeout, readTimeout);
            try {
                client.getCurrentUser();
                return client;
            } catch (NotFoundException ignored) {
                // api-endpoint not found (== api-level not supported by this client)
            }
        }

        return null;
    }

    private <R> R execute(GitLabOperation<R> operation) {
        return operation.execute(false);
    }

    private abstract class GitLabOperation<R> {
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

        abstract R execute(GitLabClient client);
    }
}
