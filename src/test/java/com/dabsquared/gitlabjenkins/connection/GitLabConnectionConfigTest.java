package com.dabsquared.gitlabjenkins.connection;

import static com.dabsquared.gitlabjenkins.connection.Messages.connection_error;
import static com.dabsquared.gitlabjenkins.connection.Messages.connection_success;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnection.DescriptorImpl;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.impl.V3GitLabClientBuilder;
import hudson.ProxyConfiguration;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jakarta.ws.rs.core.Response;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import jenkins.model.Jenkins;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.HttpRequest;

/**
 * @author Robin Müller
 */
@WithJenkins
@ExtendWith(MockServerExtension.class)
class GitLabConnectionConfigTest {

    private static final String API_TOKEN = "secret";
    private static final String API_TOKEN_ID = "apiTokenId";

    private JenkinsRule jenkins;

    private static MockServerClient mockServerClient;
    private String gitLabUrl;

    @BeforeAll
    static void setUp(MockServerClient client) {
        mockServerClient = client;
    }

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jenkins = rule;
        gitLabUrl = "http://localhost:" + mockServerClient.getPort() + "/gitlab";
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstanceOrNull())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(
                        domains.get(0),
                        new StringCredentialsImpl(
                                CredentialsScope.SYSTEM,
                                API_TOKEN_ID,
                                "GitLab API Token",
                                Secret.fromString(API_TOKEN)));
            }
        }
    }

    @AfterEach
    void tearDown() {
        mockServerClient.reset();
    }

    @Test
    void doCheckConnection_success() {
        String expected = connection_success();
        assertThat(doCheckConnection("v3", Response.Status.OK), is(expected));
        assertThat(doCheckConnection("v4", Response.Status.OK), is(expected));
    }

    @Test
    void doCheckConnection_forbidden() {
        String expected = connection_error("HTTP 403 Forbidden");
        assertThat(doCheckConnection("v3", Response.Status.FORBIDDEN), is(expected));
        assertThat(doCheckConnection("v4", Response.Status.FORBIDDEN), is(expected));
    }

    @Test
    void doCheckConnection_proxy() {
        jenkins.getInstance().proxy = new ProxyConfiguration("0.0.0.0", 80);
        GitLabConnection.DescriptorImpl descriptor =
                (DescriptorImpl) jenkins.jenkins.getDescriptor(GitLabConnection.class);
        FormValidation result = descriptor.doTestConnection(gitLabUrl, API_TOKEN_ID, "v3", false, 10, 10);
        assertThat(result.getMessage(), containsString("Connection refused"));
    }

    @Test
    void doCheckConnection_noProxy() {
        jenkins.getInstance().proxy = new ProxyConfiguration("0.0.0.0", 80, "", "", "localhost");
        assertThat(doCheckConnection("v3", Response.Status.OK), is(connection_success()));
    }

    private String doCheckConnection(String clientBuilderId, Response.Status status) {
        HttpRequest request =
                request().withPath("/gitlab/api/" + clientBuilderId + "/.*").withHeader("PRIVATE-TOKEN", API_TOKEN);
        mockServerClient.when(request).respond(response().withStatusCode(status.getStatusCode()));

        GitLabConnection.DescriptorImpl descriptor =
                (DescriptorImpl) jenkins.jenkins.getDescriptor(GitLabConnection.class);
        FormValidation formValidation =
                descriptor.doTestConnection(gitLabUrl, API_TOKEN_ID, clientBuilderId, false, 10, 10);
        mockServerClient.verify(request);
        return formValidation.getMessage();
    }

    @Test
    void authenticationEnabled_anonymous_forbidden() throws Exception {
        boolean defaultValue = jenkins.get(GitLabConnectionConfig.class).isUseAuthenticatedEndpoint();
        assertTrue(defaultValue);
        jenkins.getInstance().setAuthorizationStrategy(new GlobalMatrixAuthorizationStrategy());
        URL jenkinsURL = jenkins.getURL();
        FreeStyleProject project = jenkins.createFreeStyleProject("test");
        GitLabPushTrigger trigger = mock(GitLabPushTrigger.class);
        project.addTrigger(trigger);

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(jenkinsURL.toExternalForm() + "project/test");
        request.addHeader("X-Gitlab-Event", "Push Hook");
        request.setEntity(new StringEntity("{}"));

        CloseableHttpResponse response = client.execute(request);

        assertThat(response.getStatusLine().getStatusCode(), is(403));
    }

    @Test
    void authenticationEnabled_registered_success() throws Exception {
        String username = "test-user";
        jenkins.getInstance().setSecurityRealm(jenkins.createDummySecurityRealm());
        GlobalMatrixAuthorizationStrategy authorizationStrategy = new GlobalMatrixAuthorizationStrategy();
        authorizationStrategy.add(Item.BUILD, username);
        jenkins.getInstance().setAuthorizationStrategy(authorizationStrategy);
        URL jenkinsURL = jenkins.getURL();
        jenkins.createFreeStyleProject("test");

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(jenkinsURL.toExternalForm() + "project/test");
        request.addHeader("X-Gitlab-Event", "Push Hook");
        String auth = username + ":" + username;
        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1)));
        request.setEntity(new StringEntity("{}"));

        CloseableHttpResponse response = client.execute(request);

        assertThat(response.getStatusLine().getStatusCode(), is(200));
    }

    @Test
    void authenticationDisabled_anonymous_success() throws Exception {
        jenkins.get(GitLabConnectionConfig.class).setUseAuthenticatedEndpoint(false);
        jenkins.getInstance().setAuthorizationStrategy(new GlobalMatrixAuthorizationStrategy());
        URL jenkinsURL = jenkins.getURL();
        jenkins.createFreeStyleProject("test");

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(jenkinsURL.toExternalForm() + "project/test");
        request.addHeader("X-Gitlab-Event", "Push Hook");
        request.setEntity(new StringEntity("{}"));

        CloseableHttpResponse response = client.execute(request);

        assertThat(response.getStatusLine().getStatusCode(), is(200));
    }

    @Test
    void setConnectionsTest() {
        GitLabConnection connection1 =
                new GitLabConnection("1", "http://localhost", null, new V3GitLabClientBuilder(), false, 10, 10);
        GitLabConnection connection2 =
                new GitLabConnection("2", "http://localhost", null, new V3GitLabClientBuilder(), false, 10, 10);
        GitLabConnectionConfig config = jenkins.get(GitLabConnectionConfig.class);
        List<GitLabConnection> connectionList1 = new ArrayList<>();
        connectionList1.add(connection1);

        config.setConnections(connectionList1);
        assertThat(config.getConnections(), is(connectionList1));

        List<GitLabConnection> connectionList2 = new ArrayList<>();
        connectionList2.add(connection1);
        connectionList2.add(connection2);

        config.setConnections(connectionList2);
        assertThat(config.getConnections(), is(connectionList2));

        config.setConnections(connectionList1);
        assertThat(config.getConnections(), is(connectionList1));

        connectionList1.add(connection1);
        assertThrows(IllegalArgumentException.class, () -> config.setConnections(connectionList1));
    }

    @Test
    void getClient_is_cached() {
        GitLabConnection connection = new GitLabConnection(
                "test", "http://localhost", API_TOKEN_ID, new V3GitLabClientBuilder(), false, 10, 10);
        GitLabConnectionConfig config = jenkins.get(GitLabConnectionConfig.class);
        List<GitLabConnection> connectionList1 = new ArrayList<>();
        connectionList1.add(connection);
        config.setConnections(connectionList1);

        GitLabClient client = config.getClient(connection.getName(), null, null);
        assertNotNull(client);
        assertSame(client, config.getClient(connection.getName(), null, null));
    }
}
