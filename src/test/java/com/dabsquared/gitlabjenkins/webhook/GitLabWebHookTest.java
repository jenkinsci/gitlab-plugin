package com.dabsquared.gitlabjenkins.webhook;

import static com.dabsquared.gitlabjenkins.builder.generated.GitLabPushTriggerBuilder.gitLabPushTrigger;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnection;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;

import hudson.model.FreeStyleProject;
import hudson.plugins.git.GitSCM;

/**
 * @author Robin Müller
 */
public class GitLabWebHookTest {

    private static final String GITLAB_URL = "http://example.com";
    private static final String GITLAB_PROJECT_HTTP_URL = "http://example.com/mike/diaspora.git";
    private static final String GLOBALWEBHOOK = "globalwebhook";
    private static final GitLabConnection GITLAB_CONNECTION = new GitLabConnection("gitlab", GITLAB_URL, "", null, false, 10, 10);
    private static final GitLabConnection GITLAB_CONNECTION_GLOBALHOOK = new GitLabConnection("gitlab_globalhook", GITLAB_URL, GLOBALWEBHOOK, null, false, 10, 10);
    private static final GitLabConnection GITLAB_CONNECTION_BOGUS = new GitLabConnection("bogusGitLabConnection", "http://localhost/bogusURL", "", null, false, 10, 10);
    private static final GitLabConnection GITLAB_CONNECTION_BOGUS_GLOBALHOOK = new GitLabConnection("bogusGitLabConnection", "http://localhost/bogusURL", GLOBALWEBHOOK, null, false, 10, 10);

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Mock
    private StaplerRequest request;
    @Mock
    private StaplerResponse response;

    @Before
    public void configureGitLabConnections() {
        GitLabConnectionConfig config = jenkins.get(GitLabConnectionConfig.class);
        List<GitLabConnection> connectionList = new ArrayList<>();
        connectionList.add(GITLAB_CONNECTION);
        connectionList.add(GITLAB_CONNECTION_GLOBALHOOK);
        connectionList.add(GITLAB_CONNECTION_BOGUS);
        connectionList.add(GITLAB_CONNECTION_BOGUS_GLOBALHOOK);

        config.setConnections (connectionList);
    }

    @Test
    public void globalWebhookTrigger_push_success() throws IOException, InterruptedException{
        FreeStyleProject project = createProject("project", GITLAB_CONNECTION_GLOBALHOOK);
        project.setQuietPeriod(0);

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(jenkins.getURL().toExternalForm() + "project/" + GLOBALWEBHOOK);
        request.addHeader("X-Gitlab-Event", "Push Hook");
        request.setEntity (new InputStreamEntity (getClass().getResourceAsStream("../postPush.json")));

        CloseableHttpResponse response = client.execute(request);

        TimeUnit.SECONDS.sleep (1);

        assertThat(response.getStatusLine().getStatusCode(), is(200));
        assertNotNull(project.getLastBuild ());
    }

    @Test
    public void globalWebhookTrigger_mergeRequest_success() throws IOException, InterruptedException{
        FreeStyleProject project = createProject("project", GITLAB_CONNECTION_GLOBALHOOK);
        project.setQuietPeriod(0);

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(jenkins.getURL().toExternalForm() + "project/" + GLOBALWEBHOOK);
        request.addHeader("X-Gitlab-Event", "Merge Request Hook");
        request.setEntity (new InputStreamEntity (getClass().getResourceAsStream("../postMergeRequest.json")));

        CloseableHttpResponse response = client.execute(request);

        TimeUnit.SECONDS.sleep (1);

        assertThat(response.getStatusLine().getStatusCode(), is(200));
        assertNotNull(project.getLastBuild ());
    }

    @Test
    public void globalWebhookTrigger_note_success() throws IOException, InterruptedException{
        FreeStyleProject project = createProject("project", GITLAB_CONNECTION_GLOBALHOOK);
        project.setQuietPeriod(0);

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(jenkins.getURL().toExternalForm() + "project/" + GLOBALWEBHOOK);
        request.addHeader("X-Gitlab-Event", "Note Hook");
        request.setEntity (new InputStreamEntity (getClass().getResourceAsStream("../postNote.json")));

        CloseableHttpResponse response = client.execute(request);

        TimeUnit.SECONDS.sleep (1);

        assertThat(response.getStatusLine().getStatusCode(), is(200));
        assertNotNull(project.getLastBuild ());
    }

    @Test
    public void globalWebhookTrigger_pushTag_success() throws IOException, InterruptedException{
        FreeStyleProject project = createProject("project", GITLAB_CONNECTION_GLOBALHOOK);
        project.setQuietPeriod(0);

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(jenkins.getURL().toExternalForm() + "project/" + GLOBALWEBHOOK);
        request.addHeader("X-Gitlab-Event", "Tag Push Hook");
        request.setEntity (new InputStreamEntity (getClass().getResourceAsStream("../postPushTag.json")));

        CloseableHttpResponse response = client.execute(request);

        TimeUnit.SECONDS.sleep (1);

        assertThat(response.getStatusLine().getStatusCode(), is(200));
        assertNotNull(project.getLastBuild ());
    }

    @Test
    public void globalWebhookTrigger_failure() throws IOException, InterruptedException {
        FreeStyleProject projectNoGlobalhook = createProject("project", GITLAB_CONNECTION);
        FreeStyleProject projectWrongGitLab = createProject("projectWrongGitLab", GITLAB_CONNECTION_BOGUS_GLOBALHOOK);
        projectNoGlobalhook.setQuietPeriod(0);
        projectWrongGitLab.setQuietPeriod (0);

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(jenkins.getURL().toExternalForm() + "project/" + GLOBALWEBHOOK);
        request.addHeader("X-Gitlab-Event", "Push Hook");
        request.setEntity (new InputStreamEntity (getClass().getResourceAsStream("../postPush.json")));

        CloseableHttpResponse response = client.execute(request);

        TimeUnit.SECONDS.sleep (10);

        assertThat(response.getStatusLine().getStatusCode(), is(404));
        assertNull(projectNoGlobalhook.getLastBuild ());
        assertNull(projectWrongGitLab.getLastBuild ());
    }

    @Test
    public void webhookTrigger_success() throws IOException, InterruptedException {
        FreeStyleProject project = createProject("project", GITLAB_CONNECTION_GLOBALHOOK);
        project.setQuietPeriod(0);

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(jenkins.getURL().toExternalForm() + "project/project");
        request.addHeader("X-Gitlab-Event", "Push Hook");
        request.setEntity (new InputStreamEntity (getClass().getResourceAsStream("../postPush.json")));

        CloseableHttpResponse response = client.execute(request);

        TimeUnit.SECONDS.sleep (1);

        assertThat(response.getStatusLine().getStatusCode(), is(200));
        assertNotNull(project.getLastBuild ());
    }

    private FreeStyleProject createProject(String name, GitLabConnection gitLabConnection) throws IOException {
        FreeStyleProject project = jenkins.createFreeStyleProject(name);

        project.addProperty (new GitLabConnectionProperty(gitLabConnection.getName ()));;

        GitSCM gitSCM = new GitSCM(GITLAB_PROJECT_HTTP_URL);
        project.setScm (gitSCM);

        GitLabPushTrigger trigger = gitLabPushTrigger().withTriggerOnMergeRequest (true).withTriggerOnNoteRequest (true).withNoteRegex ("This MR needs work.").withTriggerOnPush(true).withBranchFilterType(BranchFilterType.All).build();
        project.addTrigger(trigger);
        trigger.start (project, true);
        return project;
    }
}
