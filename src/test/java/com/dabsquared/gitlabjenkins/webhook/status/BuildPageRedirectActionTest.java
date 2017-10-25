package com.dabsquared.gitlabjenkins.webhook.status;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.git.GitSCM;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * @author Robin Müller
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class BuildPageRedirectActionTest {

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
    public void redirectToBuildUrl() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.setScm(new GitSCM(gitRepoUrl));
        testProject.setQuietPeriod(0);
        QueueTaskFuture<FreeStyleBuild> future = testProject.scheduleBuild2(0);
        FreeStyleBuild build = future.get(15, TimeUnit.SECONDS);

        getBuildPageRedirectAction(testProject).execute(response);

        verify(response).sendRedirect2(jenkins.getInstance().getRootUrl() + build.getUrl());
    }

    @Test
    public void redirectToBuildStatusUrl() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.setScm(new GitSCM(gitRepoUrl));
        testProject.setQuietPeriod(0);
        QueueTaskFuture<FreeStyleBuild> future = testProject.scheduleBuild2(0);
        FreeStyleBuild build = future.get(5, TimeUnit.SECONDS);

        doThrow(IOException.class).when(response).sendRedirect2(jenkins.getInstance().getRootUrl() + build.getUrl());
        getBuildPageRedirectAction(testProject).execute(response);

        verify(response).sendRedirect2(jenkins.getInstance().getRootUrl() + build.getBuildStatusUrl());
    }

    protected abstract BuildPageRedirectAction getBuildPageRedirectAction(FreeStyleProject project);
}
