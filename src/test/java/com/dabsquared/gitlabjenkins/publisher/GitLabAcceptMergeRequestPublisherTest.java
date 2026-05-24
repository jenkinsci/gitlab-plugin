package com.dabsquared.gitlabjenkins.publisher;

import static com.dabsquared.gitlabjenkins.publisher.TestUtility.GITLAB_CONNECTION_V3;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.GITLAB_CONNECTION_V4;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.MERGE_REQUEST_ID;
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
import java.nio.charset.Charset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.HttpRequest;

/**
 * @author Nikolay Ustinov
 */
@WithJenkins
@ExtendWith(MockServerExtension.class)
class GitLabAcceptMergeRequestPublisherTest {

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
        verifyMatrixAggregatable(GitLabAcceptMergeRequestPublisher.class, listener);
    }

    @Test
    void success() throws Exception {
        publish(mockSimpleBuild(GITLAB_CONNECTION_V3, Result.SUCCESS));
        publish(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.SUCCESS));

        mockServerClient.verify(
                prepareAcceptMergeRequestWithSuccessResponse("v3", MERGE_REQUEST_ID, null),
                prepareAcceptMergeRequestWithSuccessResponse("v4", MERGE_REQUEST_IID, null));
    }

    @Test
    void failed() throws Exception {
        publish(mockSimpleBuild(GITLAB_CONNECTION_V3, Result.FAILURE));
        publish(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.FAILURE));

        mockServerClient.verifyZeroInteractions();
    }

    private void publish(AbstractBuild build) throws Exception {
        GitLabAcceptMergeRequestPublisher publisher = preparePublisher(new GitLabAcceptMergeRequestPublisher(), build);
        publisher.perform(build, null, listener);
    }

    private HttpRequest prepareAcceptMergeRequestWithSuccessResponse(
            String apiLevel, int mergeRequestId, Boolean shouldRemoveSourceBranch) {
        HttpRequest updateCommitStatus = prepareAcceptMergeRequest(apiLevel, mergeRequestId, shouldRemoveSourceBranch);
        mockServerClient.when(updateCommitStatus).respond(response().withStatusCode(200));
        return updateCommitStatus;
    }

    private HttpRequest prepareAcceptMergeRequest(String apiLevel, int mergeRequestId, Boolean removeSourceBranch) {
        String body = "merge_commit_message=Merge+Request+accepted+by+jenkins+build+success";
        if (removeSourceBranch != null) {
            body += "&should_remove_source_branch=" + removeSourceBranch;
        }
        return request()
                .withPath("/gitlab/api/" + apiLevel + "/projects/" + PROJECT_ID + "/merge_requests/" + mergeRequestId
                        + "/merge")
                .withMethod("PUT")
                .withHeader("PRIVATE-TOKEN", "secret")
                .withBody(body);
    }
}
