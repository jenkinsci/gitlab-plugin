package com.dabsquared.gitlabjenkins.connection;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
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
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
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
        HttpRequest request = request().withPath("/gitlab/api/v3/.*").withHeader("PRIVATE-TOKEN", API_TOKEN);
        mockServerClient.when(request).respond(response().withStatusCode(Response.Status.OK.getStatusCode()));

        GitLabConnectionConfig connectionConfig = jenkins.get(GitLabConnectionConfig.class);
        FormValidation formValidation = connectionConfig.doTestConnection(gitLabUrl, API_TOKEN_ID, false, 10, 10);

        assertThat(formValidation.getMessage(), is(Messages.connection_success()));
        mockServerClient.verify(request);
    }

    @Test
    public void doCheckConnection_forbidden() throws IOException {
        HttpRequest request = request().withPath("/gitlab/api/v3/.*").withHeader("PRIVATE-TOKEN", API_TOKEN);
        mockServerClient.when(request).respond(response().withStatusCode(Response.Status.FORBIDDEN.getStatusCode()));

        GitLabConnectionConfig connectionConfig = jenkins.get(GitLabConnectionConfig.class);
        FormValidation formValidation = connectionConfig.doTestConnection(gitLabUrl, API_TOKEN_ID, false, 10, 10);

        assertThat(formValidation.getMessage(), is(Messages.connection_error("HTTP 403 Forbidden")));
        mockServerClient.verify(request);
    }
}
