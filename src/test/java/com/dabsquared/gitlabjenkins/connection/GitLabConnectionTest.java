package com.dabsquared.gitlabjenkins.connection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.dabsquared.gitlabjenkins.gitlab.api.impl.V4GitLabClientBuilder;
import com.dabsquared.gitlabjenkins.testing.gitlab.rule.GitLabRule;

import hudson.util.Secret;
import java.io.IOException;
import java.util.List;
import jenkins.model.Jenkins;
import org.gitlab4j.api.GitLabApi;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class GitLabConnectionTest {
    private static final String API_TOKEN = "secret";
    private static final String API_TOKEN_ID = "apiTokenId";
    private static final String API_TOKEN_ID_2 = "apiTokenId2";

    private static final String GITLAB_URL = "http://localhost:" + System.getProperty("gitlab.http.port", "55580");

    @Rule
    public GitLabRule gitlab =
            new GitLabRule(GITLAB_URL, Integer.parseInt(System.getProperty("postgres.port", "5432")));

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

        connection = new GitLabConnection(
                "test", GITLAB_URL, API_TOKEN_ID, new V4GitLabClientBuilder(), false, 10, 10);
    }

    @Test
    public void getClient_nullCredentialId_sameClient() {
        final GitLabApi client = connection.getClient(null, null);
        assertThat(client, notNullValue());
        assertThat(connection.getClient(null, null), sameInstance(client));
    }

    @Test
    public void getClient_nullAndDefaultCredentialId_sameClient() {
        final GitLabApi client = connection.getClient(null, null);
        assertThat(client, notNullValue());
        assertThat(connection.getClient(null, API_TOKEN_ID), sameInstance(client));
    }

    @Test
    public void getClient_differentCredentialId_differentClient() {
        final GitLabApi client1 = connection.getClient(null, API_TOKEN_ID);
        assertThat(client1, notNullValue());
        final GitLabApi client2 = connection.getClient(null, API_TOKEN_ID_2);
        assertThat(client2, notNullValue());
        assertThat(client2, not((client1)));
    }
}
