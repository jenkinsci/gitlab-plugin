package com.dabsquared.gitlabjenkins.gitlab.api.impl;

import static com.dabsquared.gitlabjenkins.gitlab.api.impl.TestUtility.assertApiImpl;
import static com.dabsquared.gitlabjenkins.gitlab.api.impl.TestUtility.buildClientWithDefaults;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import hudson.ProxyConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ResteasyGitLabClientBuilderTest {

    private JenkinsRule jenkins;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @Test
    void buildClient() throws Exception {
        GitLabClientBuilder clientBuilder = new ResteasyGitLabClientBuilder("test", 0, V3GitLabApiProxy.class, null);
        assertApiImpl(buildClientWithDefaults(clientBuilder, "http://localhost/"), V3GitLabApiProxy.class);
    }

    @Test
    void buildClientWithProxy() {
        jenkins.getInstance().proxy = new ProxyConfiguration("example.com", 8080, "test", "test", "*localhost*");
        GitLabClientBuilder clientBuilder = new ResteasyGitLabClientBuilder("test", 0, V3GitLabApiProxy.class, null);
        assertNotNull(buildClientWithDefaults(clientBuilder, "http://localhost"));
    }
}
