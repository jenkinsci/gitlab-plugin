package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.junit.MockServerRule;

import static com.dabsquared.gitlabjenkins.gitlab.api.impl.TestUtility.buildClientWithDefaults;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;


public class ResteasyGitLabClientBuilderTest {
    @Rule
    public MockServerRule mockServer = new MockServerRule(this);
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void buildClient() {
        GitLabClientBuilder clientBuilder = new ResteasyGitLabClientBuilder("test", V3GitLabClientProxy.class);
        assertThat(buildClientWithDefaults(clientBuilder, "http://localhost/"), instanceOf(V3GitLabClientProxy.class));
    }
}
