package com.dabsquared.gitlabjenkins.connection;

import hudson.util.FormValidation;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author Robin Müller
 */
public class GitLabConnectionConfigTest {

    @Rule
    public MockServerRule mockServer = new MockServerRule(this);

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private MockServerClient mockServerClient;
    private String gitLabUrl;

    @Before
    public void setup() {
        gitLabUrl = "http://localhost:" + mockServer.getPort() + "/gitlab";
    }

    @Test
    public void doCheckConnection_success() {
        String apiToken = "secret";
        HttpRequest request = request().withPath("/gitlab/api/v3/.*").withHeader("PRIVATE-TOKEN", apiToken);
        mockServerClient.when(request).respond(response().withStatusCode(Response.Status.OK.getStatusCode()));

        GitLabConnectionConfig connectionConfig = jenkins.get(GitLabConnectionConfig.class);
        FormValidation formValidation = connectionConfig.doTestConnection(gitLabUrl, apiToken, false);

        assertThat(formValidation.getMessage(), is(Messages.connection_success()));
        mockServerClient.verify(request);
    }

    @Test
    public void doCheckConnection_forbidden() {
        String apiToken = "secret";
        HttpRequest request = request().withPath("/gitlab/api/v3/.*").withHeader("PRIVATE-TOKEN", apiToken);
        mockServerClient.when(request).respond(response().withStatusCode(Response.Status.FORBIDDEN.getStatusCode()));

        GitLabConnectionConfig connectionConfig = jenkins.get(GitLabConnectionConfig.class);
        FormValidation formValidation = connectionConfig.doTestConnection(gitLabUrl, apiToken, false);

        assertThat(formValidation.getMessage(), is(Messages.connection_error("HTTP 403 Forbidden")));
        mockServerClient.verify(request);
    }
}
