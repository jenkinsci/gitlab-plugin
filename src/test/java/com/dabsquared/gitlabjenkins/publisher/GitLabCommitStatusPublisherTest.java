package com.dabsquared.gitlabjenkins.publisher;


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
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMRevisionAction;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.hamcrest.CoreMatchers;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
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
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.dabsquared.gitlabjenkins.publisher.TestUtility.BUILD_URL;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.GITLAB_CONNECTION_V3;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.GITLAB_CONNECTION_V4;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.PROJECT_ID;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.setupGitLabConnections;
import static com.dabsquared.gitlabjenkins.publisher.TestUtility.verifyMatrixAggregatable;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author Robin Müller
 */
public class GitLabCommitStatusPublisherTest {
    private static final String SHA1 = "0616d12a3a24068691027a1e113147e3c1cfa2f4";

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
        verifyMatrixAggregatable(GitLabCommitStatusPublisher.class, listener);
    }

    @Test
    public void running_v3() throws UnsupportedEncodingException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V3, null, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v3", build, BuildState.running);

        prebuildAndVerify(build, listener, requests);
    }

    @Test
    public void running_v4() throws UnsupportedEncodingException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, null, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.running);

        prebuildAndVerify(build, listener, requests);
    }


    @Test
    public void runningWithLibrary() throws UnsupportedEncodingException {
        AbstractBuild build = mockBuildWithLibrary(GITLAB_CONNECTION_V4, null, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.running);

        prebuildAndVerify(build, listener, requests);
    }


    @Test
    public void runningWithDotInProjectId() throws IOException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, null, "test/project.test.git");
        HttpRequest[] requests = new HttpRequest[] {
            prepareGetProjectResponse("test/project.test"),
            prepareExistsCommitWithSuccessResponse("v4", String.valueOf(PROJECT_ID)),
            prepareUpdateCommitStatusWithSuccessResponse("v4", String.valueOf(PROJECT_ID), build, BuildState.running)
        };

        prebuildAndVerify(build, listener, requests);
    }

    @Test
    public void canceled_v3() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V3, Result.ABORTED, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v3", build, BuildState.canceled);

        performAndVerify(build, false, requests);
    }

    @Test
    public void canceled_v4() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.ABORTED, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.canceled);

        performAndVerify(build, false, requests);
    }

    @Test
    public void canceledWithLibrary() throws IOException, InterruptedException {
        AbstractBuild build = mockBuildWithLibrary(GITLAB_CONNECTION_V4, Result.ABORTED, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.canceled);

        performAndVerify(build, false, requests);
    }

    @Test
    public void success_v3() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V3, Result.SUCCESS, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v3", build, BuildState.success);

        performAndVerify(build, false, requests);
    }


    @Test
    public void success_v4() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.SUCCESS, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.success);

        performAndVerify(build, false, requests);
    }

    @Test
    public void successWithLibrary() throws IOException, InterruptedException {
        AbstractBuild build = mockBuildWithLibrary(GITLAB_CONNECTION_V4, Result.SUCCESS, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.success);

        performAndVerify(build, false, requests);
    }
    
    @Test
    public void failed_v3() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V3, Result.FAILURE, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v3", build, BuildState.failed);

        performAndVerify(build, false, requests);
    }

    @Test
    public void failed_v4() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V3, Result.FAILURE, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v3", build, BuildState.failed);

        performAndVerify(build, false, requests);
    }

    @Test
    public void failedWithLibrary() throws IOException, InterruptedException {
        AbstractBuild build = mockBuildWithLibrary(GITLAB_CONNECTION_V4, Result.FAILURE, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.failed);

        performAndVerify(build, false, requests);
    }
    
    @Test
    public void unstable() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.UNSTABLE, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.failed);

        performAndVerify(build, false, requests);
    }

    @Test
    public void unstableWithLibrary() throws IOException, InterruptedException {
        AbstractBuild build = mockBuildWithLibrary(GITLAB_CONNECTION_V4, Result.UNSTABLE, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.failed);

        performAndVerify(build, false, requests);
    }
    
    @Test
    public void unstableAsSuccess() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, Result.UNSTABLE, "test/project.git");
        HttpRequest[] requests = prepareCheckCommitAndUpdateStatusRequests("v4", build, BuildState.success);

        performAndVerify(build, true, requests);
    }

    @Test
    public void running_multipleRepos() throws UnsupportedEncodingException {
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
    public void running_commitNotExists() throws UnsupportedEncodingException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, null, "test/project.git");
        HttpRequest updateCommitStatus = prepareUpdateCommitStatusWithSuccessResponse("v4", "test/project", build, BuildState.running);

        new GitLabCommitStatusPublisher("jenkins", false).prebuild(build, listener);
        mockServerClient.verify(updateCommitStatus, VerificationTimes.exactly(0));
    }

    @Test
    public void running_failToUpdate() throws UnsupportedEncodingException {
        AbstractBuild build = mockBuild(GITLAB_CONNECTION_V4, null, "test/project.git");
        BuildListener buildListener = mock(BuildListener.class);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(buildListener.getLogger()).thenReturn(new PrintStream(outputStream));

        prepareExistsCommitWithSuccessResponse("v4", "test/project");
        HttpRequest updateCommitStatus = prepareUpdateCommitStatus("v4", "test/project", build, BuildState.running);
        mockServerClient.when(updateCommitStatus).respond(response().withStatusCode(403));

        prebuildAndVerify(build, buildListener, updateCommitStatus);
        assertThat(outputStream.toString(), CoreMatchers.containsString("Failed to update Gitlab commit status for project 'test/project': HTTP 403 Forbidden"));
    }

    private void prebuildAndVerify(AbstractBuild build, BuildListener listener, HttpRequest... requests) {
        new GitLabCommitStatusPublisher("jenkins", false).prebuild(build, listener);
        mockServerClient.verify(requests);
    }

    private void performAndVerify(AbstractBuild build, boolean markUnstableAsSuccess, HttpRequest... requests) throws InterruptedException, IOException {
        new GitLabCommitStatusPublisher("jenkins", markUnstableAsSuccess).perform(build, null, listener);
        mockServerClient.verify(requests);
    }

    private HttpRequest[] prepareCheckCommitAndUpdateStatusRequests(String apiLevel, Run<?, ?> build, BuildState buildState) throws UnsupportedEncodingException {
        return new HttpRequest[] {
            prepareExistsCommitWithSuccessResponse(apiLevel, "test/project"),
            prepareUpdateCommitStatusWithSuccessResponse(apiLevel, "test/project", build, buildState)
        };
    }

    private HttpRequest prepareUpdateCommitStatusWithSuccessResponse(String apiLevel, String projectName, Run<?, ?> build, BuildState state) throws UnsupportedEncodingException {
        HttpRequest updateCommitStatus = prepareUpdateCommitStatus(apiLevel, projectName, build, state);
        mockServerClient.when(updateCommitStatus).respond(response().withStatusCode(200));
        return updateCommitStatus;
    }


    private HttpRequest prepareUpdateCommitStatus(final String apiLevel, String projectName, Run<?, ?> build, BuildState state) throws UnsupportedEncodingException {
        return request()
                .withPath("/gitlab/api/" + apiLevel + "/projects/" + URLEncoder.encode(projectName, "UTF-8") + "/statuses/" + SHA1)
                .withMethod("POST")
                .withHeader("PRIVATE-TOKEN", "secret")
                .withBody("state=" + URLEncoder.encode(state.name(), "UTF-8") + "&context=jenkins&" + "target_url=" + URLEncoder.encode(DisplayURLProvider.get().getRunURL(build), "UTF-8") + "&description=" + URLEncoder.encode(state.name(), "UTF-8"));
    }

    private HttpRequest prepareExistsCommitWithSuccessResponse(String apiLevel, String projectName) throws UnsupportedEncodingException {
        HttpRequest existsCommit = prepareExistsCommit(apiLevel, projectName);
        mockServerClient.when(existsCommit).respond(response().withStatusCode(200));
        return existsCommit;
    }

    private HttpRequest prepareExistsCommit(String apiLevel, String projectName) throws UnsupportedEncodingException {
        return request()
                .withPath("/gitlab/api/" + apiLevel + "/projects/" + URLEncoder.encode(projectName, "UTF-8") + "/repository/commits/" + SHA1)
                .withMethod("GET")
                .withHeader("PRIVATE-TOKEN", "secret");
    }

    private HttpRequest prepareGetProjectResponse(String projectName) throws IOException {
        HttpRequest request= request()
                     .withPath("/gitlab/api/v4/projects/" + URLEncoder.encode(projectName, "UTF-8"))
                     .withMethod("GET")
                   .  withHeader("PRIVATE-TOKEN", "secret");

        HttpResponse response = response().withBody(getSingleProjectJson("GetSingleProject.json", projectName, PROJECT_ID));

        response.withHeader("Content-Type", "application/json");
        mockServerClient.when(request).respond(response.withStatusCode(200));
        return request;
    }

    private AbstractBuild mockBuild(String gitLabConnection, Result result, String... remoteUrls) {
        AbstractBuild build = mock(AbstractBuild.class);
        List<BuildData> buildDatas = new ArrayList<>();
        BuildData buildData = mock(BuildData.class);
        Revision revision = mock(Revision.class);
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
        
        //Second build data (@librabry)
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

    private String getSingleProjectJson(String name,String projectNameWithNamespace, int porjectId) throws IOException {
        String nameSpace = projectNameWithNamespace.split("/")[0];
        String projectName = projectNameWithNamespace.split("/")[1];
        return IOUtils.toString(getClass().getResourceAsStream(name))
                 .replace("${projectId}", porjectId + "")
                 .replace("${nameSpace}", nameSpace)
                 .replace("${projectName}", projectName);
    }
}
