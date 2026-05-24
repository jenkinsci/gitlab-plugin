package com.dabsquared.gitlabjenkins.publisher;

import static com.dabsquared.gitlabjenkins.publisher.TestUtility.GITLAB_CONNECTION_V3;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.GITLAB_CONNECTION_V4;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.MERGE_REQUEST_ID;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.MERGE_REQUEST_IID;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.PROJECT_ID;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.formatNote;
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
class GitLabVotePublisherTest {

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
        mockUser(1, "jenkins");
    }

    @AfterEach
    void tearDown() {
        mockServerClient.reset();
    }

    @Test
    void matrixAggregatable() throws Exception {
        verifyMatrixAggregatable(GitLabVotePublisher.class, listener);
    }

    @Test
    void success_v3() throws Exception {
        performAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V3, Result.SUCCESS), "v3", MERGE_REQUEST_ID, "thumbsup");
    }

    @Test
    void success_v4() throws Exception {
        performAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.SUCCESS), "v4", MERGE_REQUEST_IID, "thumbsup");
    }

    @Test
    void failed_v3() throws Exception {
        performAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V3, Result.FAILURE), "v3", MERGE_REQUEST_ID, "thumbsdown");
    }

    @Test
    void failed_v4() throws Exception {
        performAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.FAILURE), "v4", MERGE_REQUEST_IID, "thumbsdown");
    }

    @Test
    void removePreviousVote() throws Exception {
        // GIVEN
        AbstractBuild build = mockSimpleBuild(GITLAB_CONNECTION_V4, Result.FAILURE);
        mockAward("v4", MERGE_REQUEST_IID, 1, "thumbsdown");

        // WHEN
        performAndVerify(build, "v4", MERGE_REQUEST_IID, "thumbsdown");

        // THEN
        mockServerClient.verify(prepareSendMessageWithSuccessResponse(build, "v4", MERGE_REQUEST_IID, "thumbsdown"));
        mockServerClient.verify(
                awardEmojiRequest("v4", MERGE_REQUEST_IID, "POST").withQueryStringParameter("name", "thumbsdown"));
    }

    private void performAndVerify(AbstractBuild build, String apiLevel, int mergeRequestId, String defaultNote)
            throws Exception {
        GitLabVotePublisher publisher = preparePublisher(new GitLabVotePublisher(), build);
        publisher.perform(build, null, listener);

        mockServerClient.verify(prepareSendMessageWithSuccessResponse(build, apiLevel, mergeRequestId, defaultNote));
    }

    private HttpRequest prepareSendMessageWithSuccessResponse(
            AbstractBuild build, String apiLevel, int mergeRequestId, String body) {
        HttpRequest updateCommitStatus = prepareSendMessageStatus(apiLevel, mergeRequestId, formatNote(build, body));
        mockServerClient.when(updateCommitStatus).respond(response().withStatusCode(200));
        return updateCommitStatus;
    }

    private HttpRequest prepareSendMessageStatus(final String apiLevel, int mergeRequestId, String name) {
        return awardEmojiRequest(apiLevel, mergeRequestId, "POST").withQueryStringParameter("name", name);
    }

    private HttpRequest awardEmojiRequest(final String apiLevel, int mergeRequestId, String type) {
        return request()
                .withPath("/gitlab/api/" + apiLevel + "/projects/" + PROJECT_ID + "/merge_requests/" + mergeRequestId
                        + "/award_emoji")
                .withMethod(type)
                .withHeader("PRIVATE-TOKEN", "secret");
    }

    private void mockUser(final int id, final String username) {
        String sb = ("{\"id\": " + id) + ",\"username\": \""
                + username + "\"" + ",\"email\": \"jenkins@jenkins.io\""
                + ",\"name\": \"Ms Jenkins\"}";
        mockServerClient.when(prepareUserQuery()).respond(response(sb));
    }

    private HttpRequest prepareUserQuery() {
        return request()
                .withPath("/gitlab/api/v(3|4)/user")
                .withMethod("GET")
                .withHeader("Content-Type", "text/plain")
                .withHeader("PRIVATE-TOKEN", "secret");
    }

    private void mockAward(final String apiLevel, int mergeRequestId, int awardId, String name) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"id\": ").append(awardId);
        sb.append(",\"name\": ").append(name);
        sb.append(",\"User\": ");
        sb.append("  { \"id\": 1 }");
        sb.append("}");
        mockServerClient
                .when(awardEmojiRequest(apiLevel, mergeRequestId, "GET"))
                .respond(response(sb.toString()));
    }
}
