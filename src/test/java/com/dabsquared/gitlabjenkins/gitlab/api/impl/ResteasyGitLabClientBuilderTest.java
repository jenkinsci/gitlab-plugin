package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import hudson.ProxyConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.junit.MockServerRule;

import static com.dabsquared.gitlabjenkins.gitlab.api.impl.TestUtility.assertApiImpl;
import static com.dabsquared.gitlabjenkins.gitlab.api.impl.TestUtility.buildClientWithDefaults;
import static junit.framework.TestCase.assertNotNull;


public class ResteasyGitLabClientBuilderTest {
    @Rule
    public MockServerRule mockServer = new MockServerRule(this);
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void buildClient() throws Exception {
        GitLabClientBuilder clientBuilder = new ResteasyGitLabClientBuilder("test", 0, V3GitLabApiProxy.class, null);
        assertApiImpl(buildClientWithDefaults(clientBuilder, "http://localhost/"), V3GitLabApiProxy.class);
    }

    @Test
    public void buildClientWithProxy() throws Exception {
        jenkins.getInstance().proxy = new ProxyConfiguration("example.com", 8080, "test", "test", "*localhost*");
        GitLabClientBuilder clientBuilder = new ResteasyGitLabClientBuilder("test", 0, V3GitLabApiProxy.class, null);
        assertNotNull(buildClientWithDefaults(clientBuilder, "http://localhost"));
    }

}
