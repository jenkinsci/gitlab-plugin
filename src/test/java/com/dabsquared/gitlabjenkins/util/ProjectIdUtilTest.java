package com.dabsquared.gitlabjenkins.util;

import static com.dabsquared.gitlabjenkins.util.ProjectIdUtilTest.TestData.forRemoteUrl;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import org.gitlab4j.api.GitLabApi;
import org.junit.Before;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author Robin Müller
 */
@RunWith(Theories.class)
public class ProjectIdUtilTest {

    @Mock
    GitLabApi gitLabApi;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @DataPoints
    public static TestData[] testData = {
        forRemoteUrl("git@gitlab.com", "git@gitlab.com:test/project.git").expectProjectId("test/project"),
        forRemoteUrl("https://gitlab.com", "https://gitlab.com/test/project.git")
                .expectProjectId("test/project"),
        forRemoteUrl("https://myurl.com/gitlab", "https://myurl.com/gitlab/group/project.git")
                .expectProjectId("group/project"),
        forRemoteUrl("git@gitlab.com", "git@gitlab.com:group/subgroup/project.git")
                .expectProjectId("group/subgroup/project"),
        forRemoteUrl("https://myurl.com/gitlab", "https://myurl.com/gitlab/group/subgroup/project.git")
                .expectProjectId("group/subgroup/project"),
        forRemoteUrl("https://myurl.com", "https://myurl.com/group/subgroup/project.git")
                .expectProjectId("group/subgroup/project"),
        forRemoteUrl("https://myurl.com", "https://myurl.com/group/subgroup/subsubgroup/project.git")
                .expectProjectId("group/subgroup/subsubgroup/project"),
        forRemoteUrl("git@gitlab.com", "git@gitlab.com:group/subgroup/subsubgroup/project.git")
                .expectProjectId("group/subgroup/subsubgroup/project"),
        forRemoteUrl("http://myhost", "http://myhost.com/group/project.git").expectProjectId("group/project"),
        forRemoteUrl("", "http://myhost.com/group/project.git").expectProjectId("group/project"),
        forRemoteUrl("", "http://myhost.com:group/project.git").expectProjectId("group/project"),
    };

    @Theory
    public void retrieveProjectId(TestData testData) throws ProjectIdUtil.ProjectIdResolutionException {

        when(gitLabApi.getGitLabServerUrl()).thenReturn(testData.hostUrl);

        String projectId = ProjectIdUtil.retrieveProjectId(gitLabApi, testData.remoteUrl);

        assertThat(projectId, is(testData.expectedProjectId));
    }

    static final class TestData {

        private final String hostUrl;
        private final String remoteUrl;
        private String expectedProjectId;

        private TestData(String hostUrl, String remoteUrl) {
            this.hostUrl = hostUrl;
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

        static TestData forRemoteUrl(String baseUrl, String remoteUrl) {
            return new TestData(baseUrl, remoteUrl);
        }
    }
}
