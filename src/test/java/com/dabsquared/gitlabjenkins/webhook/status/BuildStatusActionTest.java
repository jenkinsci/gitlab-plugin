package com.dabsquared.gitlabjenkins.webhook.status;

import com.dabsquared.gitlabjenkins.testhelpers.HookTrigger;
import com.dabsquared.gitlabjenkins.testhelpers.JenkinsProjectTestFactory;
import com.dabsquared.gitlabjenkins.testhelpers.ProjectSetupResult;
import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;
import com.dabsquared.gitlabjenkins.trigger.handler.merge.MergeRequestHookTriggerHandler;
import com.dabsquared.gitlabjenkins.trigger.handler.merge.MergeRequestHookTriggerHandlerFactory;
import com.google.common.base.Predicate;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.git.GitSCM;
import hudson.util.OneShotEvent;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.when;

/**
 * @author Robin Müller
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class BuildStatusActionTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    protected String commitSha1;
    protected String branch = "master";

    @Mock
    private StaplerResponse response;

    private ProjectSetupResult result;
    private Git git;
    private RevCommit commit;

    @Before
    public void setup() throws Exception {
        Git.init().setDirectory(tmp.getRoot()).call();
        tmp.newFile("test");
        git = Git.open(tmp.getRoot());
        git.add().addFilepattern("test");
        commit = git.commit().setMessage("test").call();
        commitSha1 = commit.getId().getName();
        String gitRepoUrl = tmp.getRoot().toURI().toString();

        result = new JenkinsProjectTestFactory().createProject(jenkins, gitRepoUrl);

    }

    @Test
    public void successfulBuild() throws IOException, ExecutionException, InterruptedException, GitAPIException {
        HookTrigger.triggerHookSynchronously(result, git, commit);
        FreeStyleBuild build = (FreeStyleBuild) result.getBuildNotifier().getLastTriggeredBuild();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mockResponse(out);
        getBuildStatusAction(result.getTestProject()).execute(response);

        assertSuccessfulBuild(build, out, response);
    }

    @Test
    public void failedBuild() throws IOException, ExecutionException, InterruptedException, GitAPIException {
        result.getTestProject().getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.setResult(Result.FAILURE);
                return true;
            }
        });
        HookTrigger.triggerHookSynchronously(result, git, commit);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mockResponse(out);
        getBuildStatusAction(result.getTestProject()).execute(response);

        assertFailedBuild(result.getBuildNotifier().getLastTriggeredBuild(), out, response);
    }

    @Test
    public void runningBuild() throws IOException, ExecutionException, InterruptedException, GitAPIException {
        final OneShotEvent buildStarted = new OneShotEvent();
        final OneShotEvent keepRunning = new OneShotEvent();
        final FreeStyleProject testProject = result.getTestProject();
        testProject.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                buildStarted.signal();
                keepRunning.block();
                return true;
            }
        });
        HookTrigger.triggerHookWithoutWaitingForResult(result, git, commit);
        buildStarted.block();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mockResponse(out);
        getBuildStatusAction(testProject).execute(response);
        keepRunning.signal();

        assertRunningBuild(result.getBuildNotifier().getLastTriggeredBuild(), out, response);
    }

    @Test
    public void canceledBuild() throws IOException, ExecutionException, InterruptedException, ServletException, GitAPIException {
        final FreeStyleProject testProject = result.getTestProject();
        testProject.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                try {
                    build.doStop();
                } catch (ServletException e) {
                    throw new IOException(e);
                }
                return true;
            }
        });
        HookTrigger.triggerHookSynchronously(result, git, commit);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mockResponse(out);
        getBuildStatusAction(testProject).execute(response);

        assertCanceledBuild(result.getBuildNotifier().getLastTriggeredBuild(), out, response);
    }

    @Test
    public void unstableBuild() throws IOException, ExecutionException, InterruptedException, ServletException, GitAPIException {
        final FreeStyleProject testProject = result.getTestProject();
        testProject.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.setResult(Result.UNSTABLE);
                return true;
            }
        });

        HookTrigger.triggerHookSynchronously(result, git, commit);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mockResponse(out);
        getBuildStatusAction(testProject).execute(response);

        assertUnstableBuild(result.getBuildNotifier().getLastTriggeredBuild(), out, response);
    }

    @Test
    public void notFoundBuild() throws IOException, ExecutionException, InterruptedException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        mockResponse(out);
        getBuildStatusAction(result.getTestProject()).execute(response);

        assertNotFoundBuild(out, response);
    }

    protected abstract BuildStatusAction getBuildStatusAction(FreeStyleProject project);

    protected abstract void assertSuccessfulBuild(AbstractBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException;

    protected abstract void assertFailedBuild(AbstractBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException;

    protected abstract void assertRunningBuild(AbstractBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException;

    protected abstract void assertCanceledBuild(AbstractBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException;

    protected abstract void assertUnstableBuild(AbstractBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException;

    protected abstract void assertNotFoundBuild(ByteArrayOutputStream out, StaplerResponse response) throws IOException;

    private void mockResponse(final ByteArrayOutputStream out) throws IOException {
        ServletOutputStream servletOutputStream = new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }
        };
        when(response.getOutputStream()).thenReturn(servletOutputStream);
        when(response.getWriter()).thenReturn(new PrintWriter(servletOutputStream));
    }
}
