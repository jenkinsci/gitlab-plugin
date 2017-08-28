package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;

import java.io.IOException;
import java.util.NoSuchElementException;

import static com.dabsquared.gitlabjenkins.gitlab.api.impl.TestUtility.API_TOKEN;
import static com.dabsquared.gitlabjenkins.gitlab.api.impl.TestUtility.addGitLabApiToken;
import static com.dabsquared.gitlabjenkins.gitlab.api.impl.TestUtility.buildClientWithDefaults;
import static com.dabsquared.gitlabjenkins.gitlab.api.impl.TestUtility.responseNotFound;
import static com.dabsquared.gitlabjenkins.gitlab.api.impl.TestUtility.responseOk;
import static com.dabsquared.gitlabjenkins.gitlab.api.impl.TestUtility.versionRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.fail;


public class AutodetectGitLabClientBuilderTest {
    @Rule
    public MockServerRule mockServer = new MockServerRule(this);
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    private MockServerClient mockServerClient;
    private String gitLabUrl;
    private GitLabClientBuilder clientBuilder;
    private HttpRequest v3Request;
    private HttpRequest v4Request;

    @Before
    public void setup() throws IOException {
        gitLabUrl = "http://localhost:" + mockServer.getPort() + "/gitlab";
        addGitLabApiToken();

        clientBuilder = new AutodetectGitLabClientBuilder();

        v3Request = versionRequest(V3GitLabClientBuilder.ID);
        v4Request = versionRequest(V4GitLabClientBuilder.ID);
    }

    @Test
    public void buildClient_success_v3() {
        mockServerClient.when(v3Request).respond(responseOk());
        assertThat(buildClientWithDefaults(clientBuilder, gitLabUrl), instanceOf(V3GitLabClientProxy.class));
        mockServerClient.verify(v3Request);
    }

    @Test
    public void buildClient_success_v4() {
        mockServerClient.when(v3Request).respond(responseNotFound());
        mockServerClient.when(v4Request).respond(responseOk());
        assertThat(buildClientWithDefaults(clientBuilder, gitLabUrl), instanceOf(V4GitLabClientProxy.class));
        mockServerClient.verify(v3Request, v4Request);
    }

    @Test
    public void buildClient_no_match() {
        mockServerClient.when(v3Request).respond(responseNotFound());
        mockServerClient.when(v4Request).respond(responseNotFound());
        try {
            clientBuilder.buildClient(gitLabUrl, API_TOKEN, true, 10, 10);
            fail("buildClient should throw exception when no matching candidate is found");
        } catch (NoSuchElementException e) {
            mockServerClient.verify(v3Request, v4Request);
        }
    }
}
