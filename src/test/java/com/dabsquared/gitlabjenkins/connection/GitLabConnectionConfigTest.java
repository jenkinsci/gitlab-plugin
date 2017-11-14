package com.dabsquared.gitlabjenkins.connection;


import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.impl.V3GitLabClientBuilder;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertSame;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author Robin Müller
 */
public class GitLabConnectionConfigTest {

    private static final String API_TOKEN = "secret";
    private static final String API_TOKEN_ID = "apiTokenId";

    @Rule
    public MockServerRule mockServer = new MockServerRule(this);
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    private MockServerClient mockServerClient;
    private String gitLabUrl;

    @Before
    public void setup() throws IOException {
        gitLabUrl = "http://localhost:" + mockServer.getPort() + "/gitlab";
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstance())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(domains.get(0),
                    new StringCredentialsImpl(CredentialsScope.SYSTEM, API_TOKEN_ID, "GitLab API Token", Secret.fromString(API_TOKEN)));
            }
        }
    }

    @Test
    public void doCheckConnection_success() {
        String expected = Messages.connection_success();
        assertThat(doCheckConnection("v3", Response.Status.OK), is(expected));
        assertThat(doCheckConnection("v4", Response.Status.OK), is(expected));
    }

    @Test
    public void doCheckConnection_forbidden() throws IOException {
        String expected = Messages.connection_error("HTTP 403 Forbidden");
        assertThat(doCheckConnection("v3", Response.Status.FORBIDDEN), is(expected));
        assertThat(doCheckConnection("v4", Response.Status.FORBIDDEN), is(expected));
    }

    private String doCheckConnection(String clientBuilderId, Response.Status status) {
        HttpRequest request = request().withPath("/gitlab/api/" + clientBuilderId + "/.*").withHeader("PRIVATE-TOKEN", API_TOKEN);
        mockServerClient.when(request).respond(response().withStatusCode(status.getStatusCode()));

        GitLabConnectionConfig connectionConfig = jenkins.get(GitLabConnectionConfig.class);
        FormValidation formValidation = connectionConfig.doTestConnection(gitLabUrl, API_TOKEN_ID, clientBuilderId, false, 10, 10);
        mockServerClient.verify(request);
        return formValidation.getMessage();
    }


    @Test
    public void authenticationEnabled_anonymous_forbidden() throws IOException, URISyntaxException {
        Boolean defaultValue = jenkins.get(GitLabConnectionConfig.class).isUseAuthenticatedEndpoint();
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
    public void authenticationEnabled_registered_success() throws Exception {
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
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(Base64.encodeBase64(auth.getBytes(Charset.forName("ISO-8859-1")))));
        request.setEntity(new StringEntity("{}"));

        CloseableHttpResponse response = client.execute(request);

        assertThat(response.getStatusLine().getStatusCode(), is(200));
    }

    @Test
    public void authenticationDisabled_anonymous_success() throws IOException, URISyntaxException {
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
    public void setConnectionsTest() {
        GitLabConnection connection1 = new GitLabConnection("1", "http://localhost", null, new V3GitLabClientBuilder(), false, 10, 10);
        GitLabConnection connection2 = new GitLabConnection("2", "http://localhost", null, new V3GitLabClientBuilder(), false, 10, 10);
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
    }

    @Test
    public void getClient_is_cached() {
        GitLabConnection connection = new GitLabConnection("test", "http://localhost", API_TOKEN_ID, new V3GitLabClientBuilder(), false, 10, 10);
        GitLabConnectionConfig config = jenkins.get(GitLabConnectionConfig.class);
        List<GitLabConnection> connectionList1 = new ArrayList<>();
        connectionList1.add(connection);
        config.setConnections(connectionList1);

        GitLabClient client = config.getClient(connection.getName());
        assertNotNull(client);
        assertSame(client, config.getClient(connection.getName()));
    }
}
