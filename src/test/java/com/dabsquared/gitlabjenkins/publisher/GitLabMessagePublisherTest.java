package com.dabsquared.gitlabjenkins.publisher;


import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.plugins.git.util.BuildData;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;

import static com.dabsquared.gitlabjenkins.publisher.TestUtility.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author Nikolay Ustinov
 */
public class GitLabMessagePublisherTest {
    @ClassRule
    public static MockServerRule mockServer = new MockServerRule(new Object());

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    private MockServerClient mockServerClient;
    private BuildListener listener;

    @BeforeClass
    public static void setupClass() throws IOException {
        setupGitLabConnections(jenkins, mockServer);
    }

    @Before
    public void setup() {
        listener = new StreamBuildListener(jenkins.createTaskListener().getLogger(), Charset.defaultCharset());
        mockServerClient = new MockServerClient("localhost", mockServer.getPort());
    }

    @After
    public void cleanup() {
        mockServerClient.reset();
    }

    @Test
    public void matrixAggregatable() throws InterruptedException, IOException {
        verifyMatrixAggregatable(GitLabMessagePublisher.class, listener);
    }

    @Test
    public void canceled_v3() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V3, Result.ABORTED);
        String defaultNote = formatNote(build, ":point_up: Jenkins Build {0}\n\nResults available at: [Jenkins [{1} #{2}]]({3})");

        performAndVerify(
            build, defaultNote, false, false, false, false, false,
            prepareSendMessageWithSuccessResponse("v3", MERGE_REQUEST_ID, defaultNote));
    }

    @Test
    public void canceled_v4() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.ABORTED);
        String defaultNote = formatNote(build, ":point_up: Jenkins Build {0}\n\nResults available at: [Jenkins [{1} #{2}]]({3})");

        performAndVerify(
            build, defaultNote, false, false, false, false, false,
            prepareSendMessageWithSuccessResponse("v4", MERGE_REQUEST_IID, defaultNote));
    }


    @Test
    public void success_v3() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V3, Result.SUCCESS);
        String defaultNote = formatNote(build, ":white_check_mark: Jenkins Build {0}\n\nResults available at: [Jenkins [{1} #{2}]]({3})");

        performAndVerify(
            build, defaultNote, false, false, false, false, false,
            prepareSendMessageWithSuccessResponse("v3", MERGE_REQUEST_ID, defaultNote));
    }


    @Test
    public void success_v4() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.SUCCESS);
        String defaultNote = formatNote(build, ":white_check_mark: Jenkins Build {0}\n\nResults available at: [Jenkins [{1} #{2}]]({3})");

        performAndVerify(
            build, defaultNote, false, false, false, false, false,
            prepareSendMessageWithSuccessResponse("v4", MERGE_REQUEST_IID, defaultNote));
    }

    @Test
    public void success_withOnlyForFailure() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.SUCCESS);

        performAndVerify(build, "test", true, false, false, false, false);
    }

    @Test
    public void failed_v3() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V3, Result.FAILURE);
        String defaultNote = formatNote(build, ":negative_squared_cross_mark: Jenkins Build {0}\n\nResults available at: [Jenkins [{1} #{2}]]({3})");

        performAndVerify(
            build, defaultNote, false, false, false, false, false,
            prepareSendMessageWithSuccessResponse("v3", MERGE_REQUEST_ID, defaultNote));
    }

    @Test
    public void failed_v4() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.FAILURE);
        String defaultNote = formatNote(build, ":negative_squared_cross_mark: Jenkins Build {0}\n\nResults available at: [Jenkins [{1} #{2}]]({3})");

        performAndVerify(
            build, defaultNote, false, false, false, false, false,
            prepareSendMessageWithSuccessResponse("v4", MERGE_REQUEST_IID, defaultNote));
    }


    @Test
    public void failed_withOnlyForFailed() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.FAILURE);
        String defaultNote = formatNote(build, ":negative_squared_cross_mark: Jenkins Build {0}\n\nResults available at: [Jenkins [{1} #{2}]]({3})");

        performAndVerify(
            build, defaultNote, true, false, false, false, false,
            prepareSendMessageWithSuccessResponse("v4", MERGE_REQUEST_IID, defaultNote));
    }

    @Test
    public void canceledWithCustomNote() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.ABORTED);
        String defaultNote = "abort";

        performAndVerify(
            build, defaultNote, false, false, false, true, false,
            prepareSendMessageWithSuccessResponse("v4", MERGE_REQUEST_IID, defaultNote));
    }

    @Test
    public void successWithCustomNote() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.SUCCESS);
        String defaultNote = "success";

        performAndVerify(
            build, defaultNote, false, true, false, false, false,
            prepareSendMessageWithSuccessResponse("v4", MERGE_REQUEST_IID, defaultNote));
    }

    @Test
    public void failedWithCustomNote() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.FAILURE);
        String defaultNote = "failure";

        performAndVerify(
            build, defaultNote, false, false, true, false, false,
            prepareSendMessageWithSuccessResponse("v4", MERGE_REQUEST_IID, defaultNote));
    }

    @Test
    public void unstableWithCustomNote() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.UNSTABLE);
        String defaultNote = "unstable";

        performAndVerify(
            build, defaultNote, false, false, false, false, true,
            prepareSendMessageWithSuccessResponse("v4", MERGE_REQUEST_IID, defaultNote));
    }

    private void performAndVerify(AbstractBuild build, String note, boolean onlyForFailure, boolean replaceSuccessNote, boolean replaceFailureNote, boolean replaceAbortNote, boolean replaceUnstableNote, HttpRequest... requests) throws InterruptedException, IOException {
        String successNoteText = replaceSuccessNote ? note : null;
        String failureNoteText = replaceFailureNote ? note : null;
        String abortNoteText = replaceAbortNote ? note : null;
        String unstableNoteText = replaceUnstableNote ? note : null;
        GitLabMessagePublisher publisher = preparePublisher(new GitLabMessagePublisher(onlyForFailure, replaceSuccessNote, replaceFailureNote, replaceAbortNote, replaceUnstableNote, successNoteText, failureNoteText, abortNoteText, unstableNoteText), build);
        publisher.perform(build, null, listener);

        if (requests.length > 0) {
            mockServerClient.verify(requests);
        } else {
            mockServerClient.verifyZeroInteractions();
        }
    }

    private HttpRequest prepareSendMessageWithSuccessResponse(String apiLevel, int mergeRequestId, String body) throws UnsupportedEncodingException {
        HttpRequest updateCommitStatus = prepareSendMessageStatus(apiLevel, mergeRequestId, body);
        mockServerClient.when(updateCommitStatus).respond(response().withStatusCode(200));
        return updateCommitStatus;
    }

    private HttpRequest prepareSendMessageStatus(final String apiLevel, int mergeRequestId, String body) throws UnsupportedEncodingException {
        return request()
                .withPath("/gitlab/api/" + apiLevel + "/projects/" + PROJECT_ID + "/merge_requests/" + mergeRequestId + "/notes")
                .withMethod("POST")
                .withHeader("PRIVATE-TOKEN", "secret")
                .withBody("body=" + URLEncoder.encode(body, "UTF-8"));
    }

    private AbstractBuild mockBuild(String gitLabConnection, Result result, String... remoteUrls) {
        AbstractBuild build = mock(AbstractBuild.class);
        BuildData buildData = mock(BuildData.class);
        when(buildData.getRemoteUrls()).thenReturn(new HashSet<>(Arrays.asList(remoteUrls)));
        when(build.getAction(BuildData.class)).thenReturn(buildData);
        when(build.getResult()).thenReturn(result);
        when(build.getUrl()).thenReturn(BUILD_URL);
        when(build.getResult()).thenReturn(result);
        when(build.getNumber()).thenReturn(BUILD_NUMBER);

        AbstractProject<?, ?> project = mock(AbstractProject.class);
        when(project.getProperty(GitLabConnectionProperty.class)).thenReturn(new GitLabConnectionProperty(gitLabConnection));
        when(build.getProject()).thenReturn(project);
        EnvVars environment = mock(EnvVars.class);
        when(environment.expand(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return (String) invocation.getArguments()[0];
            }
        });
        try {
            when(build.getEnvironment(any(TaskListener.class))).thenReturn(environment);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return build;
    }
}
