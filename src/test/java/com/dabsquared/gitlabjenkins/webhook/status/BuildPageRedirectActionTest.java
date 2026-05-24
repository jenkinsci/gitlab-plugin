package com.dabsquared.gitlabjenkins.webhook.status;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.git.GitSCM;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerResponse2;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Robin Müller
 */
@WithJenkins
@ExtendWith(MockitoExtension.class)
abstract class BuildPageRedirectActionTest {

    protected static JenkinsRule jenkins;

    @TempDir
    protected File tmp;

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
    void redirectToBuildUrl() throws Exception {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.setScm(new GitSCM(gitRepoUrl));
        testProject.setQuietPeriod(0);
        QueueTaskFuture<FreeStyleBuild> future = testProject.scheduleBuild2(0);
        FreeStyleBuild build = future.get(15, TimeUnit.SECONDS);

        getBuildPageRedirectAction(testProject).execute(response);

        verify(response).sendRedirect2(jenkins.getInstance().getRootUrl() + build.getUrl());
    }

    @Test
    void redirectToBuildStatusUrl() throws Exception {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.setScm(new GitSCM(gitRepoUrl));
        testProject.setQuietPeriod(0);
        QueueTaskFuture<FreeStyleBuild> future = testProject.scheduleBuild2(0);
        FreeStyleBuild build = future.get(15, TimeUnit.SECONDS);

        doThrow(IOException.class)
                .when(response)
                .sendRedirect2(jenkins.getInstance().getRootUrl() + build.getUrl());
        getBuildPageRedirectAction(testProject).execute(response);

        verify(response).sendRedirect2(jenkins.getInstance().getRootUrl() + build.getBuildStatusUrl());
    }

    protected abstract BuildPageRedirectAction getBuildPageRedirectAction(FreeStyleProject project);
}
