package com.dabsquared.gitlabjenkins.webhook.status;

import static org.mockito.Mockito.when;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.git.GitSCM;
import hudson.util.OneShotEvent;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerResponse2;
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
abstract class BuildStatusActionTest {

    private static JenkinsRule jenkins;

    @TempDir
    private File tmp;

    protected String commitSha1;
    protected String branch = "master";

    @Mock
    private StaplerResponse2 response;

    private String gitRepoUrl;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @BeforeEach
    void setUp() throws Exception {
        Git.init().setDirectory(tmp).call();
        File.createTempFile("test", null, tmp);
        Git git = Git.open(tmp);
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setSign(false).setMessage("test").call();
        commitSha1 = commit.getId().getName();
        gitRepoUrl = tmp.toURI().toString();
    }

    @Test
    void successfulBuild() throws Exception {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.setScm(new GitSCM(gitRepoUrl));
        FreeStyleBuild build = testProject.scheduleBuild2(0).get();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mockResponse(out);
        getBuildStatusAction(testProject).execute(response);

        assertSuccessfulBuild(build, out, response);
    }

    @Test
    void failedBuild() throws Exception {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.setScm(new GitSCM(gitRepoUrl));
        testProject.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
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
    void runningBuild() throws Exception {
        final OneShotEvent buildStarted = new OneShotEvent();
        final OneShotEvent keepRunning = new OneShotEvent();
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.setScm(new GitSCM(gitRepoUrl));
        testProject.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException {
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
    void canceledBuild() throws Exception {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.setScm(new GitSCM(gitRepoUrl));
        testProject.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws IOException {
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
    void unstableBuild() throws Exception {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.setScm(new GitSCM(gitRepoUrl));
        testProject.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
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
    void notFoundBuild() throws Exception {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.setScm(new GitSCM(gitRepoUrl));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mockResponse(out);
        getBuildStatusAction(testProject).execute(response);

        assertNotFoundBuild(out, response);
    }

    protected abstract BuildStatusAction getBuildStatusAction(FreeStyleProject project);

    protected abstract void assertSuccessfulBuild(
            FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse2 response) throws Exception;

    protected abstract void assertFailedBuild(
            FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse2 response) throws Exception;

    protected abstract void assertRunningBuild(
            FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse2 response) throws Exception;

    protected abstract void assertCanceledBuild(
            FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse2 response) throws Exception;

    protected abstract void assertUnstableBuild(
            FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse2 response) throws Exception;

    protected abstract void assertNotFoundBuild(ByteArrayOutputStream out, StaplerResponse2 response) throws Exception;

    private void mockResponse(final ByteArrayOutputStream out) throws Exception {
        ServletOutputStream servletOutputStream = new ServletOutputStream() {
            @Override
            public void write(int b) {
                out.write(b);
            }

            @Override
            public void setWriteListener(jakarta.servlet.WriteListener writeListener) {}

            @Override
            public boolean isReady() {
                return true;
            }
        };
        when(response.getOutputStream()).thenReturn(servletOutputStream);
        when(response.getWriter()).thenReturn(new PrintWriter(servletOutputStream));
    }
}
