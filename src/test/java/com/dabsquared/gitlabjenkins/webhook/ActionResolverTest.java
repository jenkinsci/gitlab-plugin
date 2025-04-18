package com.dabsquared.gitlabjenkins.webhook;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import com.dabsquared.gitlabjenkins.webhook.ActionResolver.NoopAction;
import com.dabsquared.gitlabjenkins.webhook.build.MergeRequestBuildAction;
import com.dabsquared.gitlabjenkins.webhook.build.NoteBuildAction;
import com.dabsquared.gitlabjenkins.webhook.build.PushBuildAction;
import com.dabsquared.gitlabjenkins.webhook.status.BranchBuildPageRedirectAction;
import com.dabsquared.gitlabjenkins.webhook.status.BranchStatusPngAction;
import com.dabsquared.gitlabjenkins.webhook.status.CommitBuildPageRedirectAction;
import com.dabsquared.gitlabjenkins.webhook.status.CommitStatusPngAction;
import com.dabsquared.gitlabjenkins.webhook.status.StatusJsonAction;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest2;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Robin Müller
 */
@WithJenkins
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActionResolverTest {

    private static JenkinsRule jenkins;

    @Mock
    private StaplerRequest2 request;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @Test
    void getBranchBuildPageRedirect() throws Exception {
        String projectName = "getBranchBuildPageRedirect";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("");
        when(request.hasParameter("ref")).thenReturn(true);
        when(request.getMethod()).thenReturn("GET");

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(BranchBuildPageRedirectAction.class));
    }

    @Test
    void getCommitStatus() throws Exception {
        String projectName = "getCommitStatus";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("builds/1234abcd/status.json");
        when(request.getMethod()).thenReturn("GET");

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(StatusJsonAction.class));
    }

    @Test
    void getCommitBuildPageRedirect_builds() throws Exception {
        String projectName = "getCommitBuildPageRedirect_builds";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("builds/1234abcd");
        when(request.getMethod()).thenReturn("GET");

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(CommitBuildPageRedirectAction.class));
    }

    @Test
    void getCommitBuildPageRedirect_commits() throws Exception {
        String projectName = "getCommitBuildPageRedirect_commits";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("commits/7890efab");
        when(request.getMethod()).thenReturn("GET");

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(CommitBuildPageRedirectAction.class));
    }

    @Test
    void getBranchStatusPng() throws Exception {
        String projectName = "getBranchStatusPng";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("builds/status.png");
        when(request.hasParameter("ref")).thenReturn(true);
        when(request.getMethod()).thenReturn("GET");

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(BranchStatusPngAction.class));
    }

    @Test
    void getCommitStatusPng() throws Exception {
        String projectName = "getCommitStatusPng";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("builds/status.png");
        when(request.hasParameter("ref")).thenReturn(false);
        when(request.getMethod()).thenReturn("GET");

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(CommitStatusPngAction.class));
    }

    @Test
    void postMergeRequest() throws Exception {
        String projectName = "postMergeRequest";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Gitlab-Event")).thenReturn("Merge Request Hook");
        when(request.getInputStream())
                .thenReturn(new ResourceServletInputStream("ActionResolverTest_postMergeRequest.json"));

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(MergeRequestBuildAction.class));
    }

    @Test
    void postSystemHookMergeRequest() throws Exception {
        String projectName = "postSystemHookMergeRequest";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Gitlab-Event")).thenReturn("System Hook");
        when(request.getInputStream())
                .thenReturn(new ResourceServletInputStream("ActionResolverTest_postSystemHook_MergeRequest.json"));

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(MergeRequestBuildAction.class));
    }

    @Test
    void postSystemHookPush() throws Exception {
        String projectName = "postSystemHookPush";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Gitlab-Event")).thenReturn("System Hook");
        when(request.getInputStream())
                .thenReturn(new ResourceServletInputStream("ActionResolverTest_postSystemHook_Push.json"));

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(PushBuildAction.class));
    }

    @Test
    void postSystemHookPushTag() throws Exception {
        String projectName = "postSystemHookPushTag";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Gitlab-Event")).thenReturn("System Hook");
        when(request.getInputStream())
                .thenReturn(new ResourceServletInputStream("ActionResolverTest_postSystemHook_PushTag.json"));

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(PushBuildAction.class));
    }

    @Test
    void postNote() throws Exception {
        String projectName = "postNote";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Gitlab-Event")).thenReturn("Note Hook");
        when(request.getInputStream()).thenReturn(new ResourceServletInputStream("ActionResolverTest_postNote.json"));

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(NoteBuildAction.class));
    }

    @Test
    void postPush() throws Exception {
        String projectName = "postPush";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Gitlab-Event")).thenReturn("Push Hook");
        when(request.getInputStream()).thenReturn(new ResourceServletInputStream("ActionResolverTest_postPush.json"));

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(PushBuildAction.class));
    }

    @Test
    void postPushTag() throws Exception {
        String projectName = "postPushTag";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Gitlab-Event")).thenReturn("Tag Push Hook");
        when(request.getInputStream())
                .thenReturn(new ResourceServletInputStream("ActionResolverTest_postPushTag.json"));

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(PushBuildAction.class));
    }

    @Test
    void postPushMissingEventHeader() throws Exception {
        String projectName = "postPushMissingEventHeader";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Gitlab-Event")).thenReturn(null);
        when(request.getInputStream()).thenReturn(new ResourceServletInputStream("ActionResolverTest_postPush.json"));

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(NoopAction.class));
    }

    @Test
    void postPushUnsupportedEventHeader() throws Exception {
        String projectName = "postPushUnsupportedEventHeader";
        jenkins.createFreeStyleProject(projectName);
        when(request.getRestOfPath()).thenReturn("");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Gitlab-Event")).thenReturn("__Not Supported Header__");
        when(request.getInputStream()).thenReturn(new ResourceServletInputStream("ActionResolverTest_postPush.json"));

        WebHookAction resolvedAction = new ActionResolver().resolve(projectName, request);

        assertThat(resolvedAction, instanceOf(NoopAction.class));
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

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public boolean isFinished() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener var1) {}
    }
}
