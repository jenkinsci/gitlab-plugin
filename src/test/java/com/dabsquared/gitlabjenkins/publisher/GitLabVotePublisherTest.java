package com.dabsquared.gitlabjenkins.publisher;


import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.StreamBuildListener;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import static com.dabsquared.gitlabjenkins.publisher.TestUtility.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author Nikolay Ustinov
 */
public class GitLabVotePublisherTest {
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
        verifyMatrixAggregatable(GitLabVotePublisher.class, listener);
    }

    @Test
    public void success_v3() throws IOException, InterruptedException {
        performAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V3, Result.SUCCESS), "v3", MERGE_REQUEST_ID, ":+1:");
    }

    @Test
    public void success_v4() throws IOException, InterruptedException {
        performAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.SUCCESS), "v4", MERGE_REQUEST_IID, ":+1:");
    }

    @Test
    public void failed_v3() throws IOException, InterruptedException {
        performAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V3, Result.FAILURE), "v3", MERGE_REQUEST_ID, ":-1:");
    }

    @Test
    public void failed_v4() throws IOException, InterruptedException {
        performAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.FAILURE), "v4", MERGE_REQUEST_IID, ":-1:");
    }


    private void performAndVerify(AbstractBuild build, String apiLevel, int mergeRequestId, String defaultNote) throws InterruptedException, IOException {
        GitLabVotePublisher publisher = preparePublisher(new GitLabVotePublisher(), build);
        publisher.perform(build, null, listener);

        mockServerClient.verify(prepareSendMessageWithSuccessResponse(build, apiLevel, mergeRequestId, defaultNote));
    }

    private HttpRequest prepareSendMessageWithSuccessResponse(AbstractBuild build, String apiLevel, int mergeRequestId, String body) throws UnsupportedEncodingException {
        HttpRequest updateCommitStatus = prepareSendMessageStatus(apiLevel, mergeRequestId, formatNote(build, body));
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
}
