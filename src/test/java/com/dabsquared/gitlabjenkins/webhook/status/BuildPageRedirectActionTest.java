package com.dabsquared.gitlabjenkins.webhook.status;

import com.dabsquared.gitlabjenkins.testhelpers.BuildNotifier;
import com.dabsquared.gitlabjenkins.testhelpers.HookTrigger;
import com.dabsquared.gitlabjenkins.testhelpers.JenkinsProjectTestFactory;
import com.dabsquared.gitlabjenkins.testhelpers.ProjectSetupResult;
import hudson.model.FreeStyleProject;
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
    private FreeStyleProject testProject;
    private BuildNotifier buildNotifier;

    @Before
    public void setup() throws Exception {
        Git.init().setDirectory(tmp.getRoot()).call();
        tmp.newFile("test");
        final Git git = Git.open(tmp.getRoot());
        git.add().addFilepattern("test");
        final RevCommit commit = git.commit().setMessage("test").call();
        commitSha1 = commit.getId().getName();
        final String gitRepoUrl = tmp.getRoot().toURI().toString();

        ProjectSetupResult project = new JenkinsProjectTestFactory().createProject(jenkins, gitRepoUrl);
        buildNotifier = project.getBuildNotifier();
        testProject = project.getTestProject();

        HookTrigger.triggerHookSynchronously(project, git, commit);
    }


    @Test
    public void redirectToBuildUrl() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        getBuildPageRedirectAction(testProject).execute(response);

        verify(response).sendRedirect2(jenkins.getInstance().getRootUrl() + buildNotifier.getLastTriggeredBuild().getUrl());
    }

    @Test
    public void redirectToBuildStatusUrl() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        doThrow(IOException.class).when(response).sendRedirect2(jenkins.getInstance().getRootUrl() + buildNotifier.getLastTriggeredBuild().getUrl());
        getBuildPageRedirectAction(testProject).execute(response);

        verify(response).sendRedirect2(jenkins.getInstance().getRootUrl() + buildNotifier.getLastTriggeredBuild().getBuildStatusUrl());
    }

    protected abstract BuildPageRedirectAction getBuildPageRedirectAction(FreeStyleProject project);

}
