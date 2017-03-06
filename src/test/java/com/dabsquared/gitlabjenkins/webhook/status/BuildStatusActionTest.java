package com.dabsquared.gitlabjenkins.webhook.status;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.git.GitSCM;
import hudson.util.OneShotEvent;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
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

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    protected String commitSha1;
    protected String branch = "master";

    @Mock
    private StaplerResponse response;

    private String gitRepoUrl;

    @Before
    public void setup() throws Exception {
        Git.init().setDirectory(tmp.getRoot()).call();
        tmp.newFile("test");
        Git git = Git.open(tmp.getRoot());
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setMessage("test").call();
        commitSha1 = commit.getId().getName();
        gitRepoUrl = tmp.getRoot().toURI().toString();
    }

    @Test
    public void successfulBuild() throws IOException, ExecutionException, InterruptedException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.setScm(new GitSCM(gitRepoUrl));
        FreeStyleBuild build = testProject.scheduleBuild2(0).get();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mockResponse(out);
        getBuildStatusAction(testProject).execute(response);

        assertSuccessfulBuild(build, out, response);
    }

    @Test
    public void failedBuild() throws IOException, ExecutionException, InterruptedException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.setScm(new GitSCM(gitRepoUrl));
        testProject.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.setResult(Result.FAILURE);
                return true;
            }
        });
        FreeStyleBuild build = testProject.scheduleBuild2(0).get();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mockResponse(out);
        getBuildStatusAction(testProject).execute(response);

        assertFailedBuild(build, out, response);
    }

    @Test
    public void runningBuild() throws IOException, ExecutionException, InterruptedException {
        final OneShotEvent buildStarted = new OneShotEvent();
        final OneShotEvent keepRunning = new OneShotEvent();
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.setScm(new GitSCM(gitRepoUrl));
        testProject.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                buildStarted.signal();
                keepRunning.block();
                return true;
            }
        });
        FreeStyleBuild build = testProject.scheduleBuild2(0).waitForStart();
        buildStarted.block();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mockResponse(out);
        getBuildStatusAction(testProject).execute(response);
        keepRunning.signal();

        assertRunningBuild(build, out, response);
    }

    @Test
    public void canceledBuild() throws IOException, ExecutionException, InterruptedException, ServletException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.setScm(new GitSCM(gitRepoUrl));
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
        FreeStyleBuild build = testProject.scheduleBuild2(0).get();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mockResponse(out);
        getBuildStatusAction(testProject).execute(response);

        assertCanceledBuild(build, out, response);
    }

    @Test
    public void unstableBuild() throws IOException, ExecutionException, InterruptedException, ServletException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.setScm(new GitSCM(gitRepoUrl));
        testProject.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.setResult(Result.UNSTABLE);
                return true;
            }
        });
        FreeStyleBuild build = testProject.scheduleBuild2(0).get();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mockResponse(out);
        getBuildStatusAction(testProject).execute(response);

        assertUnstableBuild(build, out, response);
    }

    @Test
    public void notFoundBuild() throws IOException, ExecutionException, InterruptedException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.setScm(new GitSCM(gitRepoUrl));


        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mockResponse(out);
        getBuildStatusAction(testProject).execute(response);

        assertNotFoundBuild(out, response);
    }

    protected abstract BuildStatusAction getBuildStatusAction(FreeStyleProject project);

    protected abstract void assertSuccessfulBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException;

    protected abstract void assertFailedBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException;

    protected abstract void assertRunningBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException;

    protected abstract void assertCanceledBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException;

    protected abstract void assertUnstableBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException;

    protected abstract void assertNotFoundBuild(ByteArrayOutputStream out, StaplerResponse response) throws IOException;

    private void mockResponse(final ByteArrayOutputStream out) throws IOException {
        ServletOutputStream servletOutputStream = new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                // noop
            }
        };
        when(response.getOutputStream()).thenReturn(servletOutputStream);
        when(response.getWriter()).thenReturn(new PrintWriter(servletOutputStream));
    }
}
