package com.dabsquared.gitlabjenkins.casc;

import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import io.jenkins.plugins.casc.misc.RoundTripAbstractTest;
import jenkins.model.GlobalConfiguration;
import org.junit.Assert;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class GitLabConnectionConfigTest extends RoundTripAbstractTest {

    @Override
    protected void assertConfiguredAsExpected(RestartableJenkinsRule restartableJenkinsRule, String s) {
        boolean useAuthenticatedEndpoint = GlobalConfiguration.all()
            .get(GitLabConnectionConfig.class)
            .isUseAuthenticatedEndpoint();
        Assert.assertEquals(false, useAuthenticatedEndpoint);
    }

    @Override
    protected String stringInLogExpected() {
        return "useAuthenticatedEndpoint = false";
    }
}
