package com.dabsquared.gitlabjenkins.util;

import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.ProjectBuilder.project;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.RepositoryBuilder.repository;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.Repository;
import java.util.Collections;
import java.util.List;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.Test;

/**
 * Covers https://github.com/jenkinsci/gitlab-plugin/issues/477: a webhook's SSH url must still
 * match a job configured with an HTTPS remote (and vice versa).
 */
class RepositoryUrlUtilTest {

    @Test
    void fallsBackToGenericUrlWhenNoRemotesConfigured() throws Exception {
        Repository repository = repository()
                .withUrl("git@gitlab.org:test.git")
                .withGitSshUrl("git@gitlab.org:test.git")
                .withGitHttpUrl("https://gitlab.org/test.git")
                .build();

        URIish result = RepositoryUrlUtil.resolveUrIish(repository, (List<RemoteConfig>) null);

        assertThat(result, is(new URIish("git@gitlab.org:test.git")));
    }

    @Test
    void prefersHttpUrlWhenJobIsConfiguredOverHttp() throws Exception {
        Repository repository = repository()
                .withUrl("git@gitlab.org:test.git")
                .withGitSshUrl("git@gitlab.org:test.git")
                .withGitHttpUrl("https://gitlab.org/test.git")
                .build();

        URIish result = RepositoryUrlUtil.resolveUrIish(
                repository, Collections.singletonList(remoteConfig("https://gitlab.org/test.git")));

        assertThat(result, is(new URIish("https://gitlab.org/test.git")));
    }

    @Test
    void prefersSshUrlWhenJobIsConfiguredOverSsh() throws Exception {
        Repository repository = repository()
                .withUrl("https://gitlab.org/test.git")
                .withGitSshUrl("git@gitlab.org:test.git")
                .withGitHttpUrl("https://gitlab.org/test.git")
                .build();

        URIish result = RepositoryUrlUtil.resolveUrIish(
                repository, Collections.singletonList(remoteConfig("git@gitlab.org:test.git")));

        assertThat(result, is(new URIish("git@gitlab.org:test.git")));
    }

    @Test
    void fallsBackToGenericUrlWhenNoCandidateMatchesConfiguredRemote() throws Exception {
        Repository repository = repository()
                .withUrl("git@gitlab.org:test.git")
                .withGitSshUrl("git@gitlab.org:test.git")
                .withGitHttpUrl("https://gitlab.org/test.git")
                .build();

        URIish result = RepositoryUrlUtil.resolveUrIish(
                repository, Collections.singletonList(remoteConfig("https://example.com/other.git")));

        assertThat(result, is(new URIish("git@gitlab.org:test.git")));
    }

    @Test
    void returnsNullWhenRepositoryIsNull() {
        assertThat(RepositoryUrlUtil.resolveUrIish((Repository) null, (List<RemoteConfig>) null), is(nullValue()));
    }

    /**
     * Merge request, note and pipeline hooks don't carry a usable {@code repository} field (see
     * {@link #returnsNullWhenRepositoryIsNull()} and the lack of git URLs on a minimal one) — they
     * must resolve via the payload's {@code project} object instead.
     */
    @Test
    void project_fallsBackToGenericUrlWhenNoRemotesConfigured() throws Exception {
        Project project = project()
                .withUrl("git@gitlab.org:test.git")
                .withGitSshUrl("git@gitlab.org:test.git")
                .withGitHttpUrl("https://gitlab.org/test.git")
                .build();

        URIish result = RepositoryUrlUtil.resolveUrIish(project, (List<RemoteConfig>) null);

        assertThat(result, is(new URIish("git@gitlab.org:test.git")));
    }

    @Test
    void project_prefersHttpUrlWhenJobIsConfiguredOverHttp() throws Exception {
        Project project = project()
                .withUrl("git@gitlab.org:test.git")
                .withGitSshUrl("git@gitlab.org:test.git")
                .withGitHttpUrl("https://gitlab.org/test.git")
                .build();

        URIish result = RepositoryUrlUtil.resolveUrIish(
                project, Collections.singletonList(remoteConfig("https://gitlab.org/test.git")));

        assertThat(result, is(new URIish("https://gitlab.org/test.git")));
    }

    @Test
    void project_prefersSshUrlWhenJobIsConfiguredOverSsh() throws Exception {
        Project project = project()
                .withUrl("https://gitlab.org/test.git")
                .withGitSshUrl("git@gitlab.org:test.git")
                .withGitHttpUrl("https://gitlab.org/test.git")
                .build();

        URIish result = RepositoryUrlUtil.resolveUrIish(
                project, Collections.singletonList(remoteConfig("git@gitlab.org:test.git")));

        assertThat(result, is(new URIish("git@gitlab.org:test.git")));
    }

    @Test
    void project_fallsBackToGenericUrlWhenNoCandidateMatchesConfiguredRemote() throws Exception {
        Project project = project()
                .withUrl("git@gitlab.org:test.git")
                .withGitSshUrl("git@gitlab.org:test.git")
                .withGitHttpUrl("https://gitlab.org/test.git")
                .build();

        URIish result = RepositoryUrlUtil.resolveUrIish(
                project, Collections.singletonList(remoteConfig("https://example.com/other.git")));

        assertThat(result, is(new URIish("git@gitlab.org:test.git")));
    }

    @Test
    void returnsNullWhenProjectIsNull() {
        assertThat(RepositoryUrlUtil.resolveUrIish((Project) null, (List<RemoteConfig>) null), is(nullValue()));
    }

    private static RemoteConfig remoteConfig(String url) throws Exception {
        RemoteConfig remoteConfig = new RemoteConfig(new Config(), "origin");
        remoteConfig.addURI(new URIish(url));
        return remoteConfig;
    }
}
