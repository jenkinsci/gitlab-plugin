package com.dabsquared.gitlabjenkins.publisher;


import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.dabsquared.gitlabjenkins.connection.GitLabConnection;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.impl.V3GitLabClientBuilder;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMRevisionAction;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.hamcrest.CoreMatchers;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
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

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author Robin Müller
 */
public class GitLabCommitStatusPublisherTest {

    private static final String GIT_LAB_CONNECTION = "GitLab";
    private static final String API_TOKEN = "secret";
    private static final String SHA1 = "0616d12a3a24068691027a1e113147e3c1cfa2f4";

    @ClassRule
    public static MockServerRule mockServer = new MockServerRule(new Object());

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    private MockServerClient mockServerClient;
    private BuildListener listener;

    @BeforeClass
    public static void setupConnection() throws IOException {
        GitLabConnectionConfig connectionConfig = jenkins.get(GitLabConnectionConfig.class);
        String apiTokenId = "apiTokenId";
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstance())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(domains.get(0),
                                                new StringCredentialsImpl(CredentialsScope.SYSTEM, apiTokenId, "GitLab API Token", Secret.fromString(API_TOKEN)));
            }
        }
        connectionConfig.addConnection(new GitLabConnection(GIT_LAB_CONNECTION, "http://localhost:" + mockServer.getPort() + "/gitlab", apiTokenId, new V3GitLabClientBuilder(), false, 10, 10));
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
    public void matrixAggregatable() throws UnsupportedEncodingException, InterruptedException, IOException {
        AbstractBuild build = mock(AbstractBuild.class);
        AbstractProject project = mock(MatrixConfiguration.class);
        GitLabCommitStatusPublisher publisher = mock(GitLabCommitStatusPublisher.class);
        MatrixBuild parentBuild = mock(MatrixBuild.class);

        when(build.getParent()).thenReturn(project);
        when(publisher.createAggregator(any(MatrixBuild.class), any(Launcher.class), any(BuildListener.class))).thenCallRealMethod();
        when(publisher.perform(any(AbstractBuild.class), any(Launcher.class), any(BuildListener.class))).thenReturn(true);

        MatrixAggregator aggregator = publisher.createAggregator(parentBuild, null, listener);
        aggregator.startBuild();
        aggregator.endBuild();
        verify(publisher).perform(parentBuild, null, listener);
    }

    @Test
    public void running() throws UnsupportedEncodingException {
        HttpRequest[] requests = new HttpRequest[] {
            prepareExistsCommitWithSuccessResponse("test/project", SHA1),
            prepareUpdateCommitStatusWithSuccessResponse("test/project", SHA1, jenkins.getInstance().getRootUrl() + "/build/123", BuildState.running)
        };
        AbstractBuild build = mockBuild(SHA1, "/build/123", GIT_LAB_CONNECTION, null, "test/project.git");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher("jenkins", false);
        publisher.prebuild(build, listener);

        mockServerClient.verify(requests);
    }

    @Test
    public void runningWithLibrary() throws UnsupportedEncodingException {
        HttpRequest[] requests = new HttpRequest[] {
            prepareExistsCommitWithSuccessResponse("test/project", SHA1),
            prepareUpdateCommitStatusWithSuccessResponse("test/project", SHA1, jenkins.getInstance().getRootUrl() + "/build/123", BuildState.running)
        };
        AbstractBuild build = mockBuildWithLibrary(SHA1, "/build/123", GIT_LAB_CONNECTION, null, "test/project.git");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher("jenkins", false);
        publisher.prebuild(build, listener);

        mockServerClient.verify(requests);
    }
    
    @Test
    public void runningWithDotInProjectId() throws IOException {
        HttpRequest[] requests = new HttpRequest[] {
            prepareGetProjectResponse("test/project.test",1),
            prepareExistsCommitWithSuccessResponse("1", SHA1),
            prepareUpdateCommitStatusWithSuccessResponse("1", SHA1, jenkins.getInstance().getRootUrl() + "/build/123", BuildState.running)
        };
        AbstractBuild build = mockBuild(SHA1, "/build/123", GIT_LAB_CONNECTION, null, "test/project.test.git");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher("jenkins", false);
        publisher.prebuild(build, listener);

        mockServerClient.verify(requests);
    }

    @Test
    public void canceled() throws IOException, InterruptedException {
        HttpRequest[] requests = new HttpRequest[] {
                prepareExistsCommitWithSuccessResponse("test/project", SHA1),
                prepareUpdateCommitStatusWithSuccessResponse("test/project", SHA1, jenkins.getInstance().getRootUrl() + "/build/123", BuildState.canceled)
        };
        AbstractBuild build = mockBuild(SHA1, "/build/123", GIT_LAB_CONNECTION, Result.ABORTED, "test/project.git");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher("jenkins", false);
        publisher.perform(build, null, listener);

        mockServerClient.verify(requests);
    }

    @Test
    public void canceledWithLibrary() throws IOException, InterruptedException {
        HttpRequest[] requests = new HttpRequest[] {
                prepareExistsCommitWithSuccessResponse("test/project", SHA1),
                prepareUpdateCommitStatusWithSuccessResponse("test/project", SHA1, jenkins.getInstance().getRootUrl() + "/build/123", BuildState.canceled)
        };
        AbstractBuild build = mockBuildWithLibrary(SHA1, "/build/123", GIT_LAB_CONNECTION, Result.ABORTED, "test/project.git");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher("jenkins", false);
        publisher.perform(build, null, listener);

        mockServerClient.verify(requests);
    }
    
    @Test
    public void success() throws IOException, InterruptedException {
        HttpRequest[] requests = new HttpRequest[] {
                prepareExistsCommitWithSuccessResponse("test/project", SHA1),
                prepareUpdateCommitStatusWithSuccessResponse("test/project", SHA1, jenkins.getInstance().getRootUrl() + "/build/123", BuildState.success)
        };
        AbstractBuild build = mockBuild(SHA1, "/build/123", GIT_LAB_CONNECTION, Result.SUCCESS, "test/project.git");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher("jenkins", false);
        publisher.perform(build, null, listener);

        mockServerClient.verify(requests);
    }

    @Test
    public void successWithLibrary() throws IOException, InterruptedException {
        HttpRequest[] requests = new HttpRequest[] {
                prepareExistsCommitWithSuccessResponse("test/project", SHA1),
                prepareUpdateCommitStatusWithSuccessResponse("test/project", SHA1, jenkins.getInstance().getRootUrl() + "/build/123", BuildState.success)
        };
        AbstractBuild build = mockBuildWithLibrary(SHA1, "/build/123", GIT_LAB_CONNECTION, Result.SUCCESS, "test/project.git");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher("jenkins", false);
        publisher.perform(build, null, listener);

        mockServerClient.verify(requests);
    }
    
    @Test
    public void failed() throws IOException, InterruptedException {
        HttpRequest[] requests = new HttpRequest[] {
                prepareExistsCommitWithSuccessResponse("test/project", SHA1),
                prepareUpdateCommitStatusWithSuccessResponse("test/project", SHA1, jenkins.getInstance().getRootUrl() + "/build/123", BuildState.failed)
        };
        AbstractBuild build = mockBuild(SHA1, "/build/123", GIT_LAB_CONNECTION, Result.FAILURE, "test/project.git");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher("jenkins", false);
        publisher.perform(build, null, listener);

        mockServerClient.verify(requests);
    }

    @Test
    public void failedWithLibrary() throws IOException, InterruptedException {
        HttpRequest[] requests = new HttpRequest[] {
                prepareExistsCommitWithSuccessResponse("test/project", SHA1),
                prepareUpdateCommitStatusWithSuccessResponse("test/project", SHA1, jenkins.getInstance().getRootUrl() + "/build/123", BuildState.failed)
        };
        AbstractBuild build = mockBuildWithLibrary(SHA1, "/build/123", GIT_LAB_CONNECTION, Result.FAILURE, "test/project.git");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher("jenkins", false);
        publisher.perform(build, null, listener);

        mockServerClient.verify(requests);
    }
    
    @Test
    public void unstable() throws IOException, InterruptedException {
        HttpRequest[] requests = new HttpRequest[] {
            prepareExistsCommitWithSuccessResponse("test/project", SHA1),
            prepareUpdateCommitStatusWithSuccessResponse("test/project", SHA1, jenkins.getInstance().getRootUrl() + "/build/123", BuildState.failed)
        };
        AbstractBuild build = mockBuild(SHA1, "/build/123", GIT_LAB_CONNECTION, Result.UNSTABLE, "test/project.git");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher("jenkins", false);
        publisher.perform(build, null, listener);

        mockServerClient.verify(requests);
    }

    @Test
    public void unstableWithLibrary() throws IOException, InterruptedException {
        HttpRequest[] requests = new HttpRequest[] {
            prepareExistsCommitWithSuccessResponse("test/project", SHA1),
            prepareUpdateCommitStatusWithSuccessResponse("test/project", SHA1, jenkins.getInstance().getRootUrl() + "/build/123", BuildState.failed)
        };
        AbstractBuild build = mockBuildWithLibrary(SHA1, "/build/123", GIT_LAB_CONNECTION, Result.UNSTABLE, "test/project.git");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher("jenkins", false);
        publisher.perform(build, null, listener);

        mockServerClient.verify(requests);
    }
    
    @Test
    public void unstableAsSuccess() throws IOException, InterruptedException {
        HttpRequest[] requests = new HttpRequest[] {
            prepareExistsCommitWithSuccessResponse("test/project", SHA1),
            prepareUpdateCommitStatusWithSuccessResponse("test/project", SHA1, jenkins.getInstance().getRootUrl() + "/build/123", BuildState.success)
        };
        AbstractBuild build = mockBuild(SHA1, "/build/123", GIT_LAB_CONNECTION, Result.UNSTABLE, "test/project.git");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher("jenkins", true);
        publisher.perform(build, null, listener);

        mockServerClient.verify(requests);
    }

    @Test
    public void running_multipleRepos() throws UnsupportedEncodingException {
        HttpRequest[] requests = new HttpRequest[] {
                prepareExistsCommitWithSuccessResponse("test/project-1", SHA1),
                prepareUpdateCommitStatusWithSuccessResponse("test/project-1", SHA1, jenkins.getInstance().getRootUrl() + "/build/123", BuildState.running),
                prepareExistsCommitWithSuccessResponse("test/project-2", SHA1),
                prepareUpdateCommitStatusWithSuccessResponse("test/project-2", SHA1, jenkins.getInstance().getRootUrl() + "/build/123", BuildState.running)
        };
        AbstractBuild build = mockBuild(SHA1, "/build/123", GIT_LAB_CONNECTION, null, "test/project-1.git", "test/project-2.git");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher("jenkins", false);
        publisher.prebuild(build, listener);

        mockServerClient.verify(requests);
    }

    @Test
    public void running_commitNotExists() throws UnsupportedEncodingException {
        HttpRequest updateCommitStatus = prepareUpdateCommitStatusWithSuccessResponse("test/project", SHA1, jenkins.getInstance().getRootUrl() + "/build/123", BuildState.running);
        AbstractBuild build = mockBuild(SHA1, "/build/123", GIT_LAB_CONNECTION, null, "test/project.git");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher("jenkins", false);
        publisher.prebuild(build, listener);

        mockServerClient.verify(updateCommitStatus, VerificationTimes.exactly(0));
    }

    @Test
    public void running_failToUpdate() throws UnsupportedEncodingException {
        prepareExistsCommitWithSuccessResponse("test/project", SHA1);
        HttpRequest updateCommitStatus = prepareUpdateCommitStatus("test/project", SHA1, jenkins.getInstance().getRootUrl() + "/build/123", BuildState.running);
        mockServerClient.when(updateCommitStatus).respond(response().withStatusCode(403));
        AbstractBuild build = mockBuild(SHA1, "/build/123", GIT_LAB_CONNECTION, null, "test/project.git");
        BuildListener buildListener = mock(BuildListener.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(buildListener.getLogger()).thenReturn(new PrintStream(outputStream));

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher("jenkins", false);
        publisher.prebuild(build, buildListener);

        assertThat(outputStream.toString(), CoreMatchers.containsString("Failed to update Gitlab commit status for project 'test/project': HTTP 403 Forbidden"));
        mockServerClient.verify(updateCommitStatus);
    }


    private HttpRequest prepareUpdateCommitStatusWithSuccessResponse(String projectId, String sha, String targetUrl, BuildState state) throws UnsupportedEncodingException {
        HttpRequest updateCommitStatus = prepareUpdateCommitStatus(projectId, sha, targetUrl, state);
        mockServerClient.when(updateCommitStatus).respond(response().withStatusCode(200));
        return updateCommitStatus;
    }


    private HttpRequest prepareUpdateCommitStatus(String projectId, String sha, String targetUrl, BuildState state) throws UnsupportedEncodingException {
        return request()
                .withPath("/gitlab/api/v3/projects/" + URLEncoder.encode(projectId, "UTF-8") + "/statuses/" + sha)
                .withMethod("POST")
                .withHeader("PRIVATE-TOKEN", "secret")
                .withBody("state=" + URLEncoder.encode(state.name(), "UTF-8") + "&context=jenkins&" + "target_url=" + URLEncoder.encode(targetUrl, "UTF-8"));
    }

    private HttpRequest prepareExistsCommitWithSuccessResponse(String projectId, String sha) throws UnsupportedEncodingException {
        HttpRequest existsCommit = prepareExistsCommit(projectId, sha);
        mockServerClient.when(existsCommit).respond(response().withStatusCode(200));
        return existsCommit;
    }

    private HttpRequest prepareExistsCommit(String projectId, String sha) throws UnsupportedEncodingException {
        return request()
                .withPath("/gitlab/api/v3/projects/" + URLEncoder.encode(projectId, "UTF-8") + "/repository/commits/" + sha)
                .withMethod("GET")
                .withHeader("PRIVATE-TOKEN", "secret");
    }

    private HttpRequest prepareGetProjectResponse(String projectName, int projectId) throws IOException {
        HttpRequest request= request()
                     .withPath("/gitlab/api/v3/projects/" + URLEncoder.encode(projectName, "UTF-8"))
                     .withMethod("GET")
                   .  withHeader("PRIVATE-TOKEN", "secret");

        HttpResponse response = response().withBody(getSingleProjectJson("GetSingleProject.json",projectName,projectId));

        response.withHeader("Content-Type", "application/json");
        mockServerClient.when(request).respond(response.withStatusCode(200));
        return request;
    }

    private AbstractBuild mockBuild(String sha, String buildUrl, String gitLabConnection, Result result, String... remoteUrls) {
        AbstractBuild build = mock(AbstractBuild.class);
        List<BuildData> buildDatas = new ArrayList<BuildData>();
        BuildData buildData = mock(BuildData.class);
        Revision revision = mock(Revision.class);
        when(revision.getSha1String()).thenReturn(sha);
        when(buildData.getLastBuiltRevision()).thenReturn(revision);
        when(buildData.getRemoteUrls()).thenReturn(new HashSet<>(Arrays.asList(remoteUrls)));
        Build gitBuild = mock(Build.class);
        when(gitBuild.getMarked()).thenReturn(revision);
        when(buildData.getLastBuild(any(ObjectId.class))).thenReturn(gitBuild);
        buildDatas.add(buildData);
        when(build.getActions(BuildData.class)).thenReturn(buildDatas);
        when(build.getAction(BuildData.class)).thenReturn(buildData);
        when(build.getResult()).thenReturn(result);
        when(build.getUrl()).thenReturn(buildUrl);
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

    private AbstractBuild mockBuildWithLibrary(String sha, String buildUrl, String gitLabConnection, Result result, String... remoteUrls) {
        AbstractBuild build = mock(AbstractBuild.class);
        List<BuildData> buildDatas = new ArrayList<BuildData>();
        BuildData buildData = mock(BuildData.class);
        SCMRevisionAction scmRevisionAction = mock(SCMRevisionAction.class);
        AbstractGitSCMSource.SCMRevisionImpl revisionImpl = mock(AbstractGitSCMSource.SCMRevisionImpl.class);
        
        when(build.getAction(SCMRevisionAction.class)).thenReturn(scmRevisionAction);
        when(scmRevisionAction.getRevision()).thenReturn(revisionImpl);
        when(revisionImpl.getHash()).thenReturn(sha);
        
        Revision revision = mock(Revision.class);
        when(revision.getSha1String()).thenReturn(sha);
        when(buildData.getLastBuiltRevision()).thenReturn(revision);
        when(buildData.getRemoteUrls()).thenReturn(new HashSet<>(Arrays.asList(remoteUrls)));

        Build gitBuild = mock(Build.class);

        when(gitBuild.getMarked()).thenReturn(revision);
        when(gitBuild.getSHA1()).thenReturn(ObjectId.fromString(sha));
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
        when(build.getUrl()).thenReturn(buildUrl);

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
