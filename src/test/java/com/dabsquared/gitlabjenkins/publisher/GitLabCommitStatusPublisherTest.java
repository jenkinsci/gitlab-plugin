package com.dabsquared.gitlabjenkins.publisher;

import static com.dabsquared.gitlabjenkins.publisher.TestUtility.BUILD_URL;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.GITLAB_CONNECTION_V3;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.GITLAB_CONNECTION_V4;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.PROJECT_ID;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.setupGitLabConnections;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.verifyMatrixAggregatable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMRevisionAction;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.hamcrest.CoreMatchers;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
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
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;

/**
 * @author Robin Müller
 */
@WithJenkins
@ExtendWith(MockServerExtension.class)
class GitLabCommitStatusPublisherTest {
    private static final String SHA1 = "0616d12a3a24068691027a1e113147e3c1cfa2f4";

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
        verifyMatrixAggregatable(GitLabCommitStatusPublisher.class, listener);
    }

    @Test
    void running_v3() {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V3, null, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v3", build, BuildState.running);

        prebuildAndVerify(build, listener, requests);
    }

    @Test
    void running_v4() {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, null, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.running);

        prebuildAndVerify(build, listener, requests);
    }

    @Test
    void runningWithLibrary() {
        AbstractBuild build = mockBuildWithLibrary(GITLAB_CONNECTION_V4, null, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.running);

        prebuildAndVerify(build, listener, requests);
    }

    @Test
    void runningWithDotInProjectId() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, null, "test/project.test.git");
        HttpRequest[] requests = new HttpRequest[] {
            prepareGetProjectResponse("test/project.test"),
            prepareExistsCommitWithSuccessResponse("v4", String.valueOf(PROJECT_ID)),
            prepareUpdateCommitStatusWithSuccessResponse("v4", String.valueOf(PROJECT_ID), build, BuildState.running)
        };

        prebuildAndVerify(build, listener, requests);
    }

    @Test
    void canceled_v3() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V3, Result.ABORTED, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v3", build, BuildState.canceled);

        performAndVerify(build, false, requests);
    }

    @Test
    void canceled_v4() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.ABORTED, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.canceled);

        performAndVerify(build, false, requests);
    }

    @Test
    void canceledWithLibrary() throws Exception {
        AbstractBuild build = mockBuildWithLibrary(GITLAB_CONNECTION_V4, Result.ABORTED, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.canceled);

        performAndVerify(build, false, requests);
    }

    @Test
    void success_v3() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V3, Result.SUCCESS, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v3", build, BuildState.success);

        performAndVerify(build, false, requests);
    }

    @Test
    void success_v4() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.SUCCESS, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.success);

        performAndVerify(build, false, requests);
    }

    @Test
    void successWithLibrary() throws Exception {
        AbstractBuild build = mockBuildWithLibrary(GITLAB_CONNECTION_V4, Result.SUCCESS, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.success);

        performAndVerify(build, false, requests);
    }

    @Test
    void failed_v3() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V3, Result.FAILURE, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v3", build, BuildState.failed);

        performAndVerify(build, false, requests);
    }

    @Test
    void failed_v4() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V3, Result.FAILURE, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v3", build, BuildState.failed);

        performAndVerify(build, false, requests);
    }

    @Test
    void failedWithLibrary() throws Exception {
        AbstractBuild build = mockBuildWithLibrary(GITLAB_CONNECTION_V4, Result.FAILURE, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.failed);

        performAndVerify(build, false, requests);
    }

    @Test
    void unstable() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.UNSTABLE, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.failed);

        performAndVerify(build, false, requests);
    }

    @Test
    void unstableWithLibrary() throws Exception {
        AbstractBuild build = mockBuildWithLibrary(GITLAB_CONNECTION_V4, Result.UNSTABLE, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.failed);

        performAndVerify(build, false, requests);
    }

    @Test
    void unstableAsSuccess() throws Exception {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.UNSTABLE, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.success);

        performAndVerify(build, true, requests);
    }

    @Test
    void running_multipleRepos() {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, null, "test/project-1.git", "test/project-2.git");
        HttpRequest[] requests = new HttpRequest[] {
            prepareExistsCommitWithSuccessResponse("v4", "test/project-1"),
            prepareUpdateCommitStatusWithSuccessResponse("v4", "test/project-1", build, BuildState.running),
            prepareExistsCommitWithSuccessResponse("v4", "test/project-2"),
            prepareUpdateCommitStatusWithSuccessResponse("v4", "test/project-2", build, BuildState.running)
        };

        prebuildAndVerify(build, listener, requests);
    }

    @Test
    void running_commitNotExists() {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, null, "test/project.git");
        HttpRequest updateCommitStatus =
                prepareUpdateCommitStatusWithSuccessResponse("v4", "test/project", build, BuildState.running);

        new GitLabCommitStatusPublisher("jenkins", false).prebuild(build, listener);
        mockServerClient.verify(updateCommitStatus, VerificationTimes.exactly(0));
    }

    @Test
    void running_failToUpdate() {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, null, "test/project.git");
        BuildListener buildListener = mock(BuildListener.class);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(buildListener.getLogger()).thenReturn(new PrintStream(outputStream));

        prepareExistsCommitWithSuccessResponse("v4", "test/project");
        HttpRequest updateCommitStatus = prepareUpdateCommitStatus("v4", "test/project", build, BuildState.running);
        mockServerClient.when(updateCommitStatus).respond(response().withStatusCode(403));

        prebuildAndVerify(build, buildListener, updateCommitStatus);
        assertThat(
                outputStream.toString(),
                CoreMatchers.containsString(
                        "Failed to update GitLab commit status for project 'test/project': HTTP 403 Forbidden"));
    }

    private void prebuildAndVerify(AbstractBuild build, BuildListener listener, HttpRequest... requests) {
        new GitLabCommitStatusPublisher("jenkins", false).prebuild(build, listener);
        mockServerClient.verify(requests);
    }

    private void performAndVerify(AbstractBuild build, boolean markUnstableAsSuccess, HttpRequest... requests)
            throws Exception {
        new GitLabCommitStatusPublisher("jenkins", markUnstableAsSuccess).perform(build, null, listener);
        mockServerClient.verify(requests);
    }

    private HttpRequest[] prepareCheckCommitAndUpdateStatusRequests(
            String apiLevel, Run<?, ?> build, BuildState buildState) {
        return new HttpRequest[] {
            prepareExistsCommitWithSuccessResponse(apiLevel, "test/project"),
            prepareUpdateCommitStatusWithSuccessResponse(apiLevel, "test/project", build, buildState)
        };
    }

    private HttpRequest prepareUpdateCommitStatusWithSuccessResponse(
            String apiLevel, String projectName, Run<?, ?> build, BuildState state) {
        HttpRequest updateCommitStatus = prepareUpdateCommitStatus(apiLevel, projectName, build, state);
        mockServerClient.when(updateCommitStatus).respond(response().withStatusCode(200));
        return updateCommitStatus;
    }

    private HttpRequest prepareUpdateCommitStatus(
            final String apiLevel, String projectName, Run<?, ?> build, BuildState state) {
        return request()
                .withPath("/gitlab/api/" + apiLevel + "/projects/"
                        + URLEncoder.encode(projectName, StandardCharsets.UTF_8) + "/statuses/" + SHA1)
                .withMethod("POST")
                .withHeader("PRIVATE-TOKEN", "secret")
                .withBody("state=" + URLEncoder.encode(state.name(), StandardCharsets.UTF_8) + "&context=jenkins&"
                        + "target_url="
                        + URLEncoder.encode(DisplayURLProvider.get().getRunURL(build), StandardCharsets.UTF_8)
                        + "&description="
                        + URLEncoder.encode(state.name(), StandardCharsets.UTF_8));
    }

    private HttpRequest prepareExistsCommitWithSuccessResponse(String apiLevel, String projectName) {
        HttpRequest existsCommit = prepareExistsCommit(apiLevel, projectName);
        mockServerClient.when(existsCommit).respond(response().withStatusCode(200));
        return existsCommit;
    }

    private HttpRequest prepareExistsCommit(String apiLevel, String projectName) {
        return request()
                .withPath("/gitlab/api/" + apiLevel + "/projects/"
                        + URLEncoder.encode(projectName, StandardCharsets.UTF_8) + "/repository/commits/" + SHA1)
                .withMethod("GET")
                .withHeader("PRIVATE-TOKEN", "secret");
    }

    private HttpRequest prepareGetProjectResponse(String projectName) throws Exception {
        HttpRequest request = request()
                .withPath("/gitlab/api/v4/projects/" + URLEncoder.encode(projectName, StandardCharsets.UTF_8))
                .withMethod("GET")
                .withHeader("PRIVATE-TOKEN", "secret");

        HttpResponse response =
                response().withBody(getSingleProjectJson("GetSingleProject.json", projectName, PROJECT_ID));

        response.withHeader("Content-Type", "application/json");
        mockServerClient.when(request).respond(response.withStatusCode(200));
        return request;
    }

    private AbstractBuild mockBuild(String gitLabConnection, Result result, String... remoteUrls) {
        AbstractBuild build = mock(AbstractBuild.class);
        List<BuildData> buildDatas = new ArrayList<>();
        BuildData buildData = mock(BuildData.class);
        Revision revision = mock(Revision.class);
        when(revision.getSha1()).thenReturn(ObjectId.fromString(SHA1));
        when(revision.getSha1String()).thenReturn(SHA1);
        when(buildData.getLastBuiltRevision()).thenReturn(revision);
        when(buildData.getRemoteUrls()).thenReturn(new HashSet<>(Arrays.asList(remoteUrls)));
        Build gitBuild = mock(Build.class);
        when(gitBuild.getMarked()).thenReturn(revision);
        when(buildData.getLastBuild(any(ObjectId.class))).thenReturn(gitBuild);
        buildDatas.add(buildData);
        when(build.getActions(BuildData.class)).thenReturn(buildDatas);
        when(build.getAction(BuildData.class)).thenReturn(buildData);
        when(build.getResult()).thenReturn(result);
        when(build.getUrl()).thenReturn(BUILD_URL);
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

    private AbstractBuild mockBuildWithLibrary(String gitLabConnection, Result result, String... remoteUrls) {
        AbstractBuild build = mock(AbstractBuild.class);
        List<BuildData> buildDatas = new ArrayList<>();
        BuildData buildData = mock(BuildData.class);
        SCMRevisionAction scmRevisionAction = mock(SCMRevisionAction.class);
        AbstractGitSCMSource.SCMRevisionImpl revisionImpl = mock(AbstractGitSCMSource.SCMRevisionImpl.class);

        when(build.getAction(SCMRevisionAction.class)).thenReturn(scmRevisionAction);
        when(scmRevisionAction.getRevision()).thenReturn(revisionImpl);
        when(revisionImpl.getHash()).thenReturn(SHA1);

        Revision revision = mock(Revision.class);
        when(revision.getSha1String()).thenReturn(SHA1);
        when(buildData.getLastBuiltRevision()).thenReturn(revision);
        when(buildData.getRemoteUrls()).thenReturn(new HashSet<>(Arrays.asList(remoteUrls)));

        Build gitBuild = mock(Build.class);

        when(gitBuild.getMarked()).thenReturn(revision);
        when(gitBuild.getSHA1()).thenReturn(ObjectId.fromString(SHA1));
        when(buildData.getLastBuild(any(ObjectId.class))).thenReturn(gitBuild);
        Map<String, Build> buildsByBranchName = new HashMap<>();
        buildsByBranchName.put("develop", gitBuild);
        when(buildData.getBuildsByBranchName()).thenReturn(buildsByBranchName);
        buildDatas.add(buildData);

        // Second build data (@librabry)
        BuildData buildDataLib = mock(BuildData.class);
        Revision revisionLib = mock(Revision.class);
        when(revisionLib.getSha1String()).thenReturn("SHALIB");
        when(buildDataLib.getLastBuiltRevision()).thenReturn(revisionLib);
        Build gitBuildLib = mock(Build.class);
        when(gitBuildLib.getMarked()).thenReturn(revisionLib);
        when(buildDataLib.getLastBuild(any(ObjectId.class))).thenReturn(gitBuildLib);
        buildDatas.add(buildDataLib);

        when(build.getActions(BuildData.class)).thenReturn(buildDatas);
        when(build.getResult()).thenReturn(result);
        when(build.getUrl()).thenReturn(BUILD_URL);

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

    private String getSingleProjectJson(String name, String projectNameWithNamespace, int projectId) throws Exception {
        String nameSpace = projectNameWithNamespace.split("/")[0];
        String projectName = projectNameWithNamespace.split("/")[1];
        return IOUtils.toString(getClass().getResourceAsStream(name), StandardCharsets.UTF_8)
                .replace("${projectId}", projectId + "")
                .replace("${nameSpace}", nameSpace)
                .replace("${projectName}", projectName);
    }
}
