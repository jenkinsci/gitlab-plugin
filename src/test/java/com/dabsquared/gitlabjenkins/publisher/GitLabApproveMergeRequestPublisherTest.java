package com.dabsquared.gitlabjenkins.publisher;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.StreamBuildListener;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import static com.dabsquared.gitlabjenkins.publisher.TestUtility.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class GitLabApproveMergeRequestPublisherTest {

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
        verifyMatrixAggregatable(GitLabApproveMergeRequestPublisher.class, listener);
    }

    @Test
    public void success_approve_unstable_v4() throws IOException, InterruptedException {
        performApprovalAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.SUCCESS), "v4", MERGE_REQUEST_IID, true);
    }

    @Test
    public void success_v4() throws IOException, InterruptedException {
        performApprovalAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.SUCCESS), "v4", MERGE_REQUEST_IID, false);
    }

    @Test
    public void unstable_approve_unstable_v4() throws IOException, InterruptedException {
        performApprovalAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.UNSTABLE), "v4", MERGE_REQUEST_IID, true);
    }

    @Test
    public void unstable_dontapprove_v4() throws IOException, InterruptedException {
        performUnapprovalAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.UNSTABLE), "v4", MERGE_REQUEST_IID, false);
    }

    @Test
    public void failed_approve_unstable_v4() throws IOException, InterruptedException {
        performUnapprovalAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.FAILURE), "v4", MERGE_REQUEST_IID, true);
    }

    @Test
    public void failed_v4() throws IOException, InterruptedException {
        performUnapprovalAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.FAILURE), "v4", MERGE_REQUEST_IID, false);
    }

    private void performApprovalAndVerify(AbstractBuild build, String apiLevel, int mergeRequestId, boolean approveUnstable) throws InterruptedException, IOException {
        GitLabApproveMergeRequestPublisher publisher = preparePublisher(new GitLabApproveMergeRequestPublisher(approveUnstable), build);
        publisher.perform(build, null, listener);

        mockServerClient.verify(prepareSendApprovalWithSuccessResponse(build, apiLevel, mergeRequestId));
    }

    private void performUnapprovalAndVerify(AbstractBuild build, String apiLevel, int mergeRequestId, boolean approveUnstable) throws InterruptedException, IOException {
        GitLabApproveMergeRequestPublisher publisher = preparePublisher(new GitLabApproveMergeRequestPublisher(approveUnstable), build);
        publisher.perform(build, null, listener);

        mockServerClient.verify(prepareSendUnapprovalWithSuccessResponse(build, apiLevel, mergeRequestId));
    }

    private HttpRequest prepareSendApprovalWithSuccessResponse(AbstractBuild build, String apiLevel, int mergeRequestId) throws UnsupportedEncodingException {
        HttpRequest approvalRequest = prepareSendApproval(apiLevel, mergeRequestId);
        mockServerClient.when(approvalRequest).respond(response().withStatusCode(200));
        return approvalRequest;
    }

    private HttpRequest prepareSendUnapprovalWithSuccessResponse(AbstractBuild build, String apiLevel, int mergeRequestId) throws UnsupportedEncodingException {
        HttpRequest unapprovalRequest = prepareSendUnapproval(apiLevel, mergeRequestId);
        mockServerClient.when(unapprovalRequest).respond(response().withStatusCode(200));
        return unapprovalRequest;
    }

    private HttpRequest prepareSendApproval(final String apiLevel, int mergeRequestId) throws UnsupportedEncodingException {
        return request()
            .withPath("/gitlab/api/" + apiLevel + "/projects/" + PROJECT_ID + "/merge_requests/" + mergeRequestId + "/approve")
            .withMethod("POST")
            .withHeader("PRIVATE-TOKEN", "secret");
    }

    private HttpRequest prepareSendUnapproval(final String apiLevel, int mergeRequestId) throws UnsupportedEncodingException {
        return request()
            .withPath("/gitlab/api/" + apiLevel + "/projects/" + PROJECT_ID + "/merge_requests/" + mergeRequestId + "/unapprove")
            .withMethod("POST")
            .withHeader("PRIVATE-TOKEN", "secret");
    }

}
