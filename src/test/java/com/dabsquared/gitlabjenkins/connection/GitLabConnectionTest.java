package com.dabsquared.gitlabjenkins.connection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.util.Secret;
import java.io.IOException;
import java.util.List;
import jenkins.model.Jenkins;
import org.gitlab4j.api.GitLabApi;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class GitLabConnectionTest {
    private static final String API_TOKEN = "secret";
    private static final String API_TOKEN_ID = "apiTokenId";
    private static final String API_TOKEN_ID_2 = "apiTokenId2";

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    private static GitLabConnection connection;

    @BeforeClass
    public static void setup() throws IOException {
        for (final CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.get())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                final List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(
                        domains.get(0),
                        new StringCredentialsImpl(
                                CredentialsScope.SYSTEM,
                                API_TOKEN_ID,
                                "GitLab API Token",
                                Secret.fromString(API_TOKEN)));
                credentialsStore.addCredentials(
                        domains.get(0),
                        new StringCredentialsImpl(
                                CredentialsScope.SYSTEM,
                                API_TOKEN_ID_2,
                                "GitLab API Token 2",
                                Secret.fromString(API_TOKEN)));
            }
        }

        connection = new GitLabConnection("test", "http://localhost", API_TOKEN_ID, false, 10, 10);
    }

    @Test
    public void getGitLabApi_nullCredentialId_sameGitLabApi() {
        final GitLabApi gitLabApi = connection.getGitLabApi(null, null);
        assertThat(gitLabApi, notNullValue());
        assertThat(connection.getGitLabApi(null, null), sameInstance(gitLabApi));
    }

    @Test
    public void getGitLabApi_nullAndDefaultCredentialId_sameGitLabApi() {
        final GitLabApi gitLabApi = connection.getGitLabApi(null, null);
        assertThat(gitLabApi, notNullValue());
        assertThat(connection.getGitLabApi(null, API_TOKEN_ID), sameInstance(gitLabApi));
    }

    @Test
    public void getGitLabApi_differentCredentialId_differentGitLabApi() {
        final GitLabApi gitLabApi1 = connection.getGitLabApi(null, API_TOKEN_ID);
        assertThat(gitLabApi1, notNullValue());
        final GitLabApi gitLabApi2 = connection.getGitLabApi(null, API_TOKEN_ID_2);
        assertThat(gitLabApi2, notNullValue());
        assertThat(gitLabApi2, not((gitLabApi1)));
    }
}
