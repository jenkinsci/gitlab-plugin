package com.dabsquared.gitlabjenkins.gitlab.api.impl;

import static com.dabsquared.gitlabjenkins.gitlab.api.impl.TestUtility.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.matchers.Times.once;

import com.dabsquared.gitlabjenkins.connection.GitlabCredentialResolver;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
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

@WithJenkins
@ExtendWith(MockServerExtension.class)
class AutodetectingGitLabClientTest {

    private JenkinsRule jenkins;

    private static MockServerClient mockServerClient;
    private String gitLabUrl;
    private GitLabClientBuilder clientBuilder;
    private AutodetectingGitLabClient api;
    private HttpRequest v3Request;
    private HttpRequest v4Request;

    @BeforeAll
    static void setUp(MockServerClient client) {
        mockServerClient = client;
    }

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jenkins = rule;

        gitLabUrl = "http://localhost:" + mockServerClient.getPort() + "/gitlab";
        addGitLabApiToken();

        List<GitLabClientBuilder> builders = Arrays.asList(new V3GitLabClientBuilder(), new V4GitLabClientBuilder());
        api = new AutodetectingGitLabClient(
                builders, gitLabUrl, new GitlabCredentialResolver(null, API_TOKEN_ID), true, 10, 10);

        v3Request = versionRequest(V3GitLabApiProxy.ID);
        v4Request = versionRequest(V4GitLabApiProxy.ID);
    }

    @AfterEach
    void tearDown() {
        mockServerClient.reset();
    }

    @Test
    void buildClient_success_v3() throws Exception {
        mockServerClient.when(v3Request).respond(responseOk());
        api.getCurrentUser();
        assertApiImpl(api, V3GitLabApiProxy.class);
        mockServerClient.verify(v3Request, v3Request);
    }

    @Test
    void buildClient_success_v4() throws Exception {
        mockServerClient.when(v3Request).respond(responseNotFound());
        mockServerClient.when(v4Request).respond(responseOk());
        api.getCurrentUser();
        assertApiImpl(api, V4GitLabApiProxy.class);
        mockServerClient.verify(v3Request, v4Request, v4Request);
    }

    @Test
    void buildClient_success_switching_apis() throws Exception {
        mockServerClient.when(v3Request, once()).respond(responseNotFound());
        mockServerClient.when(v4Request, exactly(2)).respond(responseOk());
        api.getCurrentUser();
        assertApiImpl(api, V4GitLabApiProxy.class);

        mockServerClient.when(v4Request, once()).respond(responseNotFound());
        mockServerClient.when(v3Request, exactly(2)).respond(responseOk());
        api.getCurrentUser();
        assertApiImpl(api, V3GitLabApiProxy.class);

        mockServerClient.verify(v3Request, v4Request, v4Request, v3Request, v3Request);
    }

    @Test
    void buildClient_no_match() {
        mockServerClient.when(v3Request).respond(responseNotFound());
        mockServerClient.when(v4Request).respond(responseNotFound());

        assertThrows(NoSuchElementException.class, () -> api.getCurrentUser());
        mockServerClient.verify(v3Request, v4Request);
    }
}
