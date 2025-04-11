package com.dabsquared.gitlabjenkins.publisher;

import static com.dabsquared.gitlabjenkins.publisher.TestUtility.BUILD_NUMBER;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.BUILD_URL;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.GITLAB_CONNECTION_V3;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.GITLAB_CONNECTION_V4;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.MERGE_REQUEST_ID;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.MERGE_REQUEST_IID;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.PROJECT_ID;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.formatNote;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.preparePublisher;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.setupGitLabConnections;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.verifyMatrixAggregatable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.plugins.git.util.BuildData;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.stubbing.Answer;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.HttpRequest;

/**
 * @author Nikolay Ustinov
 */
@WithJenkins
@ExtendWith(MockServerExtension.class)
class GitLabMessagePublisherTest {

    private static JenkinsRule jenkins;

    private static MockServerClient mockServerClient;
    private BuildListener listener;

    @BeforeAll
    static void setUp(JenkinsRule rule, MockServerClient client) throws Exception {
        jenkins = rule;
        mockServerClient = client;
        setupGitLabConnections(jenkins, client);
    }

    @BeforeEach
    void setUp() {
        listener = new StreamBuildListener(jenkins.createTaskListener().getLogger(), Charset.defaultCharset());
    }

    @AfterEach
    void tearDown() {
        mockServerClient.reset();
    }

    @Test
    void matrixAggregatable() throws Exception {
        verifyMatrixAggregatable(GitLabMessagePublisher.class, listener);
    }

    @Test
    void canceled_v3() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V3, Result.ABORTED);
        String defaultNote =
                formatNote(build, ":point_up: Jenkins Build {0}\n\nResults available at: [Jenkins [{1} #{2}]]({3})");

        performAndVerify(
                build,
                defaultNote,
                false,
                false,
                false,
                false,
                false,
                prepareSendMessageWithSuccessResponse("v3", MERGE_REQUEST_ID, defaultNote));
    }

    @Test
    void canceled_v4() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.ABORTED);
        String defaultNote =
                formatNote(build, ":point_up: Jenkins Build {0}\n\nResults available at: [Jenkins [{1} #{2}]]({3})");

        performAndVerify(
                build,
                defaultNote,
                false,
                false,
                false,
                false,
                false,
                prepareSendMessageWithSuccessResponse("v4", MERGE_REQUEST_IID, defaultNote));
    }

    @Test
    void success_v3() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V3, Result.SUCCESS);
        String defaultNote = formatNote(
                build, ":white_check_mark: Jenkins Build {0}\n\nResults available at: [Jenkins [{1} #{2}]]({3})");

        performAndVerify(
                build,
                defaultNote,
                false,
                false,
                false,
                false,
                false,
                prepareSendMessageWithSuccessResponse("v3", MERGE_REQUEST_ID, defaultNote));
    }

    @Test
    void success_v4() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.SUCCESS);
        String defaultNote = formatNote(
                build, ":white_check_mark: Jenkins Build {0}\n\nResults available at: [Jenkins [{1} #{2}]]({3})");

        performAndVerify(
                build,
                defaultNote,
                false,
                false,
                false,
                false,
                false,
                prepareSendMessageWithSuccessResponse("v4", MERGE_REQUEST_IID, defaultNote));
    }

    @Test
    void success_withOnlyForFailure() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.SUCCESS);

        performAndVerify(build, "test", true, false, false, false, false);
    }

    @Test
    void failed_v3() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V3, Result.FAILURE);
        String defaultNote =
                formatNote(build, ":x: Jenkins Build {0}\n\nResults available at: [Jenkins [{1} #{2}]]({3})");

        performAndVerify(
                build,
                defaultNote,
                false,
                false,
                false,
                false,
                false,
                prepareSendMessageWithSuccessResponse("v3", MERGE_REQUEST_ID, defaultNote));
    }

    @Test
    void failed_v4() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.FAILURE);
        String defaultNote =
                formatNote(build, ":x: Jenkins Build {0}\n\nResults available at: [Jenkins [{1} #{2}]]({3})");

        performAndVerify(
                build,
                defaultNote,
                false,
                false,
                false,
                false,
                false,
                prepareSendMessageWithSuccessResponse("v4", MERGE_REQUEST_IID, defaultNote));
    }

    @Test
    void failed_withOnlyForFailed() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.FAILURE);
        String defaultNote =
                formatNote(build, ":x: Jenkins Build {0}\n\nResults available at: [Jenkins [{1} #{2}]]({3})");

        performAndVerify(
                build,
                defaultNote,
                true,
                false,
                false,
                false,
                false,
                prepareSendMessageWithSuccessResponse("v4", MERGE_REQUEST_IID, defaultNote));
    }

    @Test
    void canceledWithCustomNote() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.ABORTED);
        String defaultNote = "abort";

        performAndVerify(
                build,
                defaultNote,
                false,
                false,
                false,
                true,
                false,
                prepareSendMessageWithSuccessResponse("v4", MERGE_REQUEST_IID, defaultNote));
    }

    @Test
    void successWithCustomNote() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.SUCCESS);
        String defaultNote = "success";

        performAndVerify(
                build,
                defaultNote,
                false,
                true,
                false,
                false,
                false,
                prepareSendMessageWithSuccessResponse("v4", MERGE_REQUEST_IID, defaultNote));
    }

    @Test
    void failedWithCustomNote() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.FAILURE);
        String defaultNote = "failure";

        performAndVerify(
                build,
                defaultNote,
                false,
                false,
                true,
                false,
                false,
                prepareSendMessageWithSuccessResponse("v4", MERGE_REQUEST_IID, defaultNote));
    }

    @Test
    void unstableWithCustomNote() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.UNSTABLE);
        String defaultNote = "unstable";

        performAndVerify(
                build,
                defaultNote,
                false,
                false,
                false,
                false,
                true,
                prepareSendMessageWithSuccessResponse("v4", MERGE_REQUEST_IID, defaultNote));
    }

    private void performAndVerify(
            AbstractBuild build,
            String note,
            boolean onlyForFailure,
            boolean replaceSuccessNote,
            boolean replaceFailureNote,
            boolean replaceAbortNote,
            boolean replaceUnstableNote,
            HttpRequest... requests)
            throws Exception {
        String successNoteText = replaceSuccessNote ? note : null;
        String failureNoteText = replaceFailureNote ? note : null;
        String abortNoteText = replaceAbortNote ? note : null;
        String unstableNoteText = replaceUnstableNote ? note : null;
        GitLabMessagePublisher publisher = preparePublisher(
                new GitLabMessagePublisher(
                        onlyForFailure,
                        replaceSuccessNote,
                        replaceFailureNote,
                        replaceAbortNote,
                        replaceUnstableNote,
                        successNoteText,
                        failureNoteText,
                        abortNoteText,
                        unstableNoteText),
                build);
        publisher.perform(build, null, listener);

        if (requests.length > 0) {
            mockServerClient.verify(requests);
        } else {
            mockServerClient.verifyZeroInteractions();
        }
    }

    private HttpRequest prepareSendMessageWithSuccessResponse(String apiLevel, int mergeRequestId, String body) {
        HttpRequest updateCommitStatus = prepareSendMessageStatus(apiLevel, mergeRequestId, body);
        mockServerClient.when(updateCommitStatus).respond(response().withStatusCode(200));
        return updateCommitStatus;
    }

    private HttpRequest prepareSendMessageStatus(final String apiLevel, int mergeRequestId, String body) {
        return request()
                .withPath("/gitlab/api/" + apiLevel + "/projects/" + PROJECT_ID + "/merge_requests/" + mergeRequestId
                        + "/notes")
                .withMethod("POST")
                .withHeader("PRIVATE-TOKEN", "secret")
                .withBody("body=" + URLEncoder.encode(body, StandardCharsets.UTF_8));
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
        when(project.getProperty(GitLabConnectionProperty.class))
                .thenReturn(new GitLabConnectionProperty(gitLabConnection));
        doReturn(project).when(build).getParent();
        doReturn(project).when(build).getProject();
        EnvVars environment = mock(EnvVars.class);
        when(environment.expand(anyString()))
                .thenAnswer((Answer<String>) invocation -> (String) invocation.getArguments()[0]);
        try {
            when(build.getEnvironment(any(TaskListener.class))).thenReturn(environment);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return build;
    }
}
