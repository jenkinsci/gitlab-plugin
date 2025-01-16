package com.dabsquared.gitlabjenkins.connection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.impl.V3GitLabClientBuilder;
import hudson.model.Item;
import hudson.security.Permission;
import hudson.util.Secret;
import java.io.IOException;
import java.util.List;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

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

        connection = new GitLabConnection(
                "test", "http://localhost", API_TOKEN_ID, new V3GitLabClientBuilder(), false, 10, 10);
    }

    @Test
    public void doFillApiTokenIdItemsTestAdmin() throws Exception {
        String response = retrieveResponseForUserFromFillApiTokenIdItems("admin", Jenkins.ADMINISTER);
        assertThat(response, containsString("gitlab-1"));
    }

    @Test
    @Issue("SECURITY-3260")
    public void doFillApiTokenIdItemsTestConfigure() throws Exception {
        String response = retrieveResponseForUserFromFillApiTokenIdItems("config", Jenkins.READ, Item.CONFIGURE);
        assertThat(response, containsString("\"values\":[]"));
    }

    @Test
    public void doFillApiTokenIdItemsTestRead() throws Exception {
        String response = retrieveResponseForUserFromFillApiTokenIdItems("dev", Jenkins.READ);
        assertThat(response, not(containsString("gitlab-1")));
    }

    private String retrieveResponseForUserFromFillApiTokenIdItems(String userName, Permission... permissionList)
            throws Exception {
        jenkins.jenkins.setSecurityRealm(jenkins.createDummySecurityRealm());
        jenkins.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(permissionList)
                .everywhere()
                .to(userName));

        StringCredentialsImpl gitLabToken = new StringCredentialsImpl(
                CredentialsScope.GLOBAL, "gitlab-1", "token", Secret.fromString("gitlab-token"));
        SystemCredentialsProvider.getInstance().getCredentials().add(gitLabToken);

        try (JenkinsRule.WebClient webClient = jenkins.createWebClient()) {
            webClient.login(userName);
            return webClient
                    .goTo(
                            "descriptorByName/com.dabsquared.gitlabjenkins.connection.GitLabConnection/fillApiTokenIdItems?url=http://xyz.com",
                            "application/json")
                    .getWebResponse()
                    .getContentAsString();
        }
    }

    @Test
    public void getClient_nullCredentialId_sameClient() {
        final GitLabClient client = connection.getClient(null, null);
        assertThat(client, notNullValue());
        assertThat(connection.getClient(null, null), sameInstance(client));
    }

    @Test
    public void getClient_nullAndDefaultCredentialId_sameClient() {
        final GitLabClient client = connection.getClient(null, null);
        assertThat(client, notNullValue());
        assertThat(connection.getClient(null, API_TOKEN_ID), sameInstance(client));
    }

    @Test
    public void getClient_differentCredentialId_differentClient() {
        final GitLabClient client1 = connection.getClient(null, API_TOKEN_ID);
        assertThat(client1, notNullValue());
        final GitLabClient client2 = connection.getClient(null, API_TOKEN_ID_2);
        assertThat(client2, notNullValue());
        assertThat(client2, not((client1)));
    }
}
