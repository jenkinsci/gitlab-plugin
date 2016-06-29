package com.dabsquared.gitlabjenkins.util;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static com.dabsquared.gitlabjenkins.util.ProjectIdUtilTest.TestData.forRemoteUrl;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Robin Müller
 */
@RunWith(Theories.class)
public class ProjectIdUtilTest {

    @DataPoints
    public static TestData[] testData = {
        forRemoteUrl("git@gitlab.com:test/project.git").expectProjectId("test/project"),
        forRemoteUrl("https://gitlab.com/test/project.git").expectProjectId("test/project"),
        forRemoteUrl("https://myurl.com/gitlab/group/project.git").expectProjectId("group/project")
    };

    @Theory
    public void retrieveProjectId(TestData testData) throws ProjectIdUtil.ProjectIdResolutionException {
        String projectId = ProjectIdUtil.retrieveProjectId(testData.remoteUrl);

        assertThat(projectId, is(testData.expectedProjectId));
    }


    static final class TestData {

        private final String remoteUrl;
        private String expectedProjectId;

        private TestData(String remoteUrl) {
            this.remoteUrl = remoteUrl;
        }

        private TestData expectProjectId(String expectedProjectId) {
            this.expectedProjectId = expectedProjectId;
            return this;
        }

        @Override
        public String toString() {
            return remoteUrl;
        }

        static TestData forRemoteUrl(String remoteUrl) {
            return new TestData(remoteUrl);
        }
    }
}
