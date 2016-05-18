package com.dabsquared.gitlabjenkins.publisher;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.dabsquared.gitlabjenkins.connection.GitLabConnection;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildData;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.hamcrest.CoreMatchers;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

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

    private static final String GIT_LAB_CONNECTION = "GitLab";
    private static final String API_TOKEN = "secret";

    @Rule
    public MockServerRule mockServer = new MockServerRule(this);

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private MockServerClient mockServerClient;
    private BuildListener listener;

    @Before
    public void setup() throws IOException {
        listener = new StreamBuildListener(jenkins.createTaskListener().getLogger(), Charset.defaultCharset());
        GitLabConnectionConfig connectionConfig = jenkins.get(GitLabConnectionConfig.class);
        String apiTokenId = "apiTokenId";
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstance())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(domains.get(0),
                    new StringCredentialsImpl(CredentialsScope.SYSTEM, apiTokenId, "GitLab API Token", Secret.fromString(API_TOKEN)));
            }
        }
        connectionConfig.addConnection(new GitLabConnection(GIT_LAB_CONNECTION, "http://localhost:" + mockServer.getPort() + "/gitlab", apiTokenId, false));
    }

    @After
    public void cleanup() {
        mockServerClient.reset();
    }

    @Test
    public void running() throws UnsupportedEncodingException {
        HttpRequest[] requests = new HttpRequest[] {
                prepareGetProjectResponse("test/project",1),
                prepareExistsCommitWithSuccessResponse("1", "123abc"),
                prepareUpdateCommitStatusWithSuccessResponse("1", "123abc", jenkins.getInstance().getRootUrl() + "/build/123", BuildState.running)
        };
        AbstractBuild build = mockBuild("123abc", "/build/123", GIT_LAB_CONNECTION, null, "test/project");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher();
        publisher.prebuild(build, listener);

        mockServerClient.verify(requests);
    }

    @Test
    public void canceled() throws IOException, InterruptedException {
        HttpRequest[] requests = new HttpRequest[] {
                prepareGetProjectResponse("test/project",1),
                prepareExistsCommitWithSuccessResponse("1", "123abc"),
                prepareUpdateCommitStatusWithSuccessResponse("1", "123abc", jenkins.getInstance().getRootUrl() + "/build/123", BuildState.canceled)
        };
        AbstractBuild build = mockBuild("123abc", "/build/123", GIT_LAB_CONNECTION, Result.ABORTED, "test/project");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher();
        publisher.perform(build, null, listener);

        mockServerClient.verify(requests);
    }

    @Test
    public void success() throws IOException, InterruptedException {
        HttpRequest[] requests = new HttpRequest[] {
                prepareGetProjectResponse("test/project",1),
                prepareExistsCommitWithSuccessResponse("1", "123abc"),
                prepareUpdateCommitStatusWithSuccessResponse("1", "123abc", jenkins.getInstance().getRootUrl() + "/build/123", BuildState.success)
        };
        AbstractBuild build = mockBuild("123abc", "/build/123", GIT_LAB_CONNECTION, Result.SUCCESS, "test/project");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher();
        publisher.perform(build, null, listener);

        mockServerClient.verify(requests);
    }

    @Test
    public void failed() throws IOException, InterruptedException {
        HttpRequest[] requests = new HttpRequest[] {
                prepareGetProjectResponse("test/project",1),
                prepareExistsCommitWithSuccessResponse("1", "123abc"),
                prepareUpdateCommitStatusWithSuccessResponse("1", "123abc", jenkins.getInstance().getRootUrl() + "/build/123", BuildState.failed)
        };
        AbstractBuild build = mockBuild("123abc", "/build/123", GIT_LAB_CONNECTION, Result.FAILURE, "test/project");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher();
        publisher.perform(build, null, listener);

        mockServerClient.verify(requests);
    }

    @Test
    public void running_multipleRepos() throws UnsupportedEncodingException {
        HttpRequest[] requests = new HttpRequest[] {
                prepareGetProjectResponse("test/project-1",1),
                prepareGetProjectResponse("test/project-2",2),
                prepareExistsCommitWithSuccessResponse("1", "123abc"),
                prepareUpdateCommitStatusWithSuccessResponse("1", "123abc", jenkins.getInstance().getRootUrl() + "/build/123", BuildState.running),
                prepareExistsCommitWithSuccessResponse("2", "123abc"),
                prepareUpdateCommitStatusWithSuccessResponse("2", "123abc", jenkins.getInstance().getRootUrl() + "/build/123", BuildState.running)
        };
        AbstractBuild build = mockBuild("123abc", "/build/123", GIT_LAB_CONNECTION, null, "test/project-1", "test/project-2");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher();
        publisher.prebuild(build, listener);

        mockServerClient.verify(requests);
    }

    @Test
    public void running_commitNotExists() throws UnsupportedEncodingException {
        prepareGetProjectResponse("test/project",1);
        HttpRequest updateCommitStatus = prepareUpdateCommitStatusWithSuccessResponse("1", "123abc", jenkins.getInstance().getRootUrl() + "/build/123", BuildState.running);
        AbstractBuild build = mockBuild("123abc", "/build/123", GIT_LAB_CONNECTION, null, "test/project");

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher();
        publisher.prebuild(build, listener);

        mockServerClient.verify(updateCommitStatus, VerificationTimes.exactly(0));
    }

    @Test
    public void running_failToUpdate() throws UnsupportedEncodingException {
        prepareGetProjectResponse("test/project",1);
        prepareExistsCommitWithSuccessResponse("1", "123abc");
        HttpRequest updateCommitStatus = prepareUpdateCommitStatus("1", "123abc", jenkins.getInstance().getRootUrl() + "/build/123", BuildState.running);
        mockServerClient.when(updateCommitStatus).respond(response().withStatusCode(403));
        AbstractBuild build = mockBuild("123abc", "/build/123", GIT_LAB_CONNECTION, null, "test/project");
        BuildListener buildListener = mock(BuildListener.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(buildListener.getLogger()).thenReturn(new PrintStream(outputStream));

        GitLabCommitStatusPublisher publisher = new GitLabCommitStatusPublisher();
        publisher.prebuild(build, buildListener);

        assertThat(outputStream.toString(), CoreMatchers.containsString("Failed to update Gitlab commit status for project '1': HTTP 403 Forbidden"));
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
                .withQueryStringParameter("state", state.name())
                .withQueryStringParameter("context", "jenkins")
                .withQueryStringParameter("target_url", targetUrl);
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

    private HttpRequest prepareGetProjectResponse(String projectName, int projectId) throws UnsupportedEncodingException {
      HttpRequest request= request()
               .withPath("/gitlab/api/v3/projects/" + URLEncoder.encode(projectName, "UTF-8"))
               .withMethod("GET")
               .withHeader("PRIVATE-TOKEN", "secret");

        HttpResponse response = response().withBody(
            "{\"id\":"+projectId+","+
            "\"description\":\"\","+
            "\"default_branch\":\"master\","+
            "\"tag_list\":[],"+
            "\"public\":false,"+
            "\"archived\":false,"+
            "\"visibility_level\":10,"+
            "\"ssh_url_to_repo\":\"\","+
            "\"http_url_to_repo\":\"http://localhost/"+projectName+".git\","+
            "\"web_url\":\"https://git.netcracker.com/"+projectName+"\","+
            "\"name\":\"persistence-framework\","+
            "\"name_with_namespace\":\""+projectName+"\","+
            "\"path\":\""+projectName.split("/")[1]+"\","+
            "\"path_with_namespace\":\""+projectName+"\","+
            "\"issues_enabled\":true,"+
            "\"merge_requests_enabled\":true,"+
            "\"wiki_enabled\":true,"+
            "\"builds_enabled\":true,"+
            "\"snippets_enabled\":false,"+
            "\"created_at\":\"2016-04-08T13:20:40.951Z\","+
            "\"last_activity_at\":\"2016-05-17T15:03:48.452Z\","+
            "\"shared_runners_enabled\":true,"+
            "\"creator_id\":1,"+
            "\"namespace\":{\"id\":159,"+
            "\"name\":\""+projectName.split("/")[0]+"\","+
            "\"path\":\""+projectName.split("/")[0]+"\","+
            "\"owner_id\":null,"+
            "\"created_at\":\"2016-04-08T13:18:08.233Z\","+
            "\"updated_at\":\"2016-04-08T13:18:08.233Z\","+
            "\"description\":\"\","+
            "\"avatar\":{\"url\":null},"+
            "\"share_with_group_lock\":false,"+
            "\"visibility_level\":20},"+
            "\"avatar_url\":null,"+
            "\"star_count\":2,"+
            "\"forks_count\":0,"+
            "\"open_issues_count\":0,"+
            "\"runners_token\":\"token\","+
            "\"public_builds\":true,"+
            "\"permissions\":{\"project_access\":{\"access_level\":40,"+
            "\"notification_level\":3},"+
            "\"group_access\":null}}"

        );
        response.withHeader("Content-Type","application/json");
        mockServerClient.when(request).respond(response.withStatusCode(200));
        return request;
    }

    private AbstractBuild mockBuild(String sha, String buildUrl, String gitLabConnection, Result result, String... remoteUrls) {
        AbstractBuild build = mock(AbstractBuild.class);
        BuildData buildData = mock(BuildData.class);
        Revision revision = mock(Revision.class);
        when(revision.getSha1String()).thenReturn(sha);
        when(buildData.getLastBuiltRevision()).thenReturn(revision);
        when(buildData.getRemoteUrls()).thenReturn(new HashSet<>(Arrays.asList(remoteUrls)));
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
}
