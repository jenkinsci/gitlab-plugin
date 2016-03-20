package com.dabsquared.gitlabjenkins.webhook;

import com.dabsquared.gitlabjenkins.webhook.build.MergeRequestBuildAction;
import com.dabsquared.gitlabjenkins.webhook.build.PushBuildAction;
import com.dabsquared.gitlabjenkins.webhook.status.BranchBuildPageRedirectAction;
import com.dabsquared.gitlabjenkins.webhook.status.BranchStatusPngAction;
import com.dabsquared.gitlabjenkins.webhook.status.CommitBuildPageRedirectAction;
import com.dabsquared.gitlabjenkins.webhook.status.CommitStatusPngAction;
import com.dabsquared.gitlabjenkins.webhook.status.StatusJsonAction;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Robin Müller
 */
@RunWith(MockitoJUnitRunner.class)
public class ActionResolverTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Mock
    private StaplerRequest request;

    @Test
    public void getBranchBuildPageRedirect() throws IOException {
        String projectName = "test";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("");
        when(request.hasParameter("ref")).thenReturn(true);
        when(request.getMethod()).thenReturn("GET");

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(BranchBuildPageRedirectAction.class));
    }

    @Test
    public void getCommitStatus() throws IOException {
        String projectName = "test";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("builds/1234abcd/status.json");
        when(request.getMethod()).thenReturn("GET");

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(StatusJsonAction.class));
    }

    @Test
    public void getCommitBuildPageRedirect_builds() throws IOException {
        String projectName = "test";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("builds/1234abcd");
        when(request.getMethod()).thenReturn("GET");

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(CommitBuildPageRedirectAction.class));
    }

    @Test
    public void getCommitBuildPageRedirect_commits() throws IOException {
        String projectName = "test";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("commits/7890efab");
        when(request.getMethod()).thenReturn("GET");

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(CommitBuildPageRedirectAction.class));
    }

    @Test
    public void getBranchStatusPng() throws IOException {
        String projectName = "test";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("builds/status.png");
        when(request.hasParameter("ref")).thenReturn(true);
        when(request.getMethod()).thenReturn("GET");

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(BranchStatusPngAction.class));
    }

    @Test
    public void getCommitStatusPng() throws IOException {
        String projectName = "test";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("builds/status.png");
        when(request.hasParameter("ref")).thenReturn(false);
        when(request.getMethod()).thenReturn("GET");

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(CommitStatusPngAction.class));
    }

    @Test
    public void postMergeRequest() throws IOException {
        String projectName = "test";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Gitlab-Event")).thenReturn("Merge Request Hook");
        when(request.getInputStream()).thenReturn(new ResourceServletInputStream("ActionResolverTest_postMergeRequest.json"));

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(MergeRequestBuildAction.class));
    }

    @Test
    public void postPush() throws IOException {
        String projectName = "test";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Gitlab-Event")).thenReturn("Push Hook");
        when(request.getInputStream()).thenReturn(new ResourceServletInputStream("ActionResolverTest_postPush.json"));

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(PushBuildAction.class));
    }


    private static class ResourceServletInputStream extends ServletInputStream {

        private final InputStream input;

        private ResourceServletInputStream(String classResourceName) {
            this.input = getClass().getResourceAsStream(classResourceName);
        }

        @Override
        public int read() throws IOException {
            return input.read();
        }
    }
}
