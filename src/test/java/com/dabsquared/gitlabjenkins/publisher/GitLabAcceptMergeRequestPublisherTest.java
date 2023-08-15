package com.dabsquared.gitlabjenkins.publisher;

import static com.dabsquared.gitlabjenkins.publisher.TestUtility.GITLAB_CONNECTION_V4;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.MERGE_REQUEST_IID;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.PROJECT_ID;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.mockSimpleBuild;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.preparePublisher;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.setupGitLabConnections;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.verifyMatrixAggregatable;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.StreamBuildListener;
import java.io.IOException;
import java.nio.charset.Charset;
import org.gitlab4j.api.GitLabApiException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.*;

/**
 * @author Nikolay Ustinov
 */
public class GitLabAcceptMergeRequestPublisherTest {
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
        verifyMatrixAggregatable(GitLabAcceptMergeRequestPublisher.class, listener);
    }

    @Test
    public void success() throws IOException, InterruptedException, GitLabApiException {
        publish(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.SUCCESS));

        mockServerClient.verify(
            prepareAcceptMergeRequestWithSuccessResponse("V4", MERGE_REQUEST_IID, null));
    }

    @Test
    public void failed() throws IOException, InterruptedException, GitLabApiException {
        publish(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.FAILURE));

        mockServerClient.verifyZeroInteractions();
    }

    private void publish(AbstractBuild<?, ?> build) throws InterruptedException, IOException, GitLabApiException {
        GitLabAcceptMergeRequestPublisher publisher = preparePublisher(new GitLabAcceptMergeRequestPublisher(), build);
        publisher.perform(build, null, listener);
    }

    private HttpRequest prepareAcceptMergeRequestWithSuccessResponse(
            String apiLevel, Long mergeRequestId, Boolean shouldRemoveSourceBranch) {
        HttpRequest updateCommitStatus = prepareAcceptMergeRequest(apiLevel, mergeRequestId, shouldRemoveSourceBranch);
        mockServerClient.when(updateCommitStatus).respond(response().withStatusCode(200));
        return updateCommitStatus;
    }

    private HttpRequest prepareAcceptMergeRequest(String apiLevel, Long mergeRequestId, Boolean removeSourceBranch) {
        String string = "merge_commit_message=Merge+Request+accepted+by+jenkins+build+success&merge_when_pipeline_succeeds=true";
        if (removeSourceBranch != null) {
            string += "&should_remove_source_branch=" + removeSourceBranch;
        }
        return request()
            .withPath("/gitlab/api/" + apiLevel + "/projects/" + PROJECT_ID + "/merge_requests/" + mergeRequestId
                    + "/merge")
            .withMethod("PUT")
            .withHeader("PRIVATE-TOKEN", "secret")
            .withHeader("Accept", "application/json")
            .withHeader("User-Agent", "Jersey/2.40 (HttpUrlConnection 11.0.17)")
            .withHeader("Connection", "keep-alive")
            .withHeader("Content-Type", "application/x-www-form-urlencoded")
            .withHeader("Host", "localhost:" + mockServer.getPort())
            .withHeader("Content-Length", String.valueOf(string.length()))
            .withSecure(false)
            .withKeepAlive(true)
            .withBody(new StringBody(string, new MediaType(
                    "application",
                    "x-www-form-urlencoded"
            )));

    }
}
