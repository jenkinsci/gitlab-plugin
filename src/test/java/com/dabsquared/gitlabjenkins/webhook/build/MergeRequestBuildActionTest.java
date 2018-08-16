package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import hudson.model.*;
import hudson.model.queue.QueueListener;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.git.GitSCM;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.ResponseImpl;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Robin Müller
 */
@RunWith(MockitoJUnitRunner.class)
public class MergeRequestBuildActionTest {

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Mock
    private StaplerResponse response;

    private static volatile boolean wouldFire = false;

    private GitLabPushTrigger trigger = new GitLabPushTrigger();

    private String gitRepoUrl;
    private String commitSha1;

    @BeforeClass
    public static void addQueueListener() {
        QueueListener ql = new QueueListener() {
            @Override
            public void onEnterWaiting(Queue.WaitingItem wi) {
                System.out.println("Got "+wi+" : "+wi.getCausesDescription());
                wouldFire = true;
            }

            @Override
            public void onEnterBuildable(Queue.BuildableItem bi) {
                System.out.println("Is buildable: "+bi.getCausesDescription());
            }
        };
        jenkins.getInstance().getExtensionList(QueueListener.class).add(ql);
    }

    @Before
    public void setup() throws Exception {
        Git.init().setDirectory(tmp.getRoot()).call();
        tmp.newFile("test");
        Git git = Git.open(tmp.getRoot());
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setMessage("test").call();
        commitSha1 = commit.getId().getName();
        gitRepoUrl = tmp.getRoot().toURI().toString();

        // some defaults of the trigger
        trigger.setBranchFilterType(BranchFilterType.All);
    }


    @Test
    public void build() throws IOException {
        GitLabPushTrigger mockTrigger = mock(GitLabPushTrigger.class);
        try {
            FreeStyleProject testProject = jenkins.createFreeStyleProject();
            testProject.addTrigger(mockTrigger);
            executeMergeRequestAction(testProject, getJson("MergeRequestEvent.json"));
        } finally {
            ArgumentCaptor<MergeRequestHook> pushHookArgumentCaptor = ArgumentCaptor.forClass(MergeRequestHook.class);
            verify(mockTrigger).onPost(pushHookArgumentCaptor.capture());
            assertThat(pushHookArgumentCaptor.getValue().getProject(), is(notNullValue()));
            assertThat(pushHookArgumentCaptor.getValue().getProject().getWebUrl(), is(notNullValue()));
        }
    }

    private void executeMergeRequestAction(FreeStyleProject testProject, String json) throws IOException {
        try {
            wouldFire = false;

            trigger.start(testProject, false);

            new MergeRequestBuildAction(testProject, json, null)
                .execute(response);
        } catch (HttpResponses.HttpResponseException hre) {
            // Test for OK status of a response.
            try {
                hre.generateResponse(null, response, null);
                verify(response).setStatus(200);
            } catch (ServletException e) {
                throw new IOException(e);
            }
        }
        // The assumption is, that queue listener have already been invoked when we got back a response.
    }

    @Test
    public void skip_closedMR() throws IOException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);

        executeMergeRequestAction(testProject, getJson("MergeRequestEvent_closedMR.json"));
        assertFalse(wouldFire);
    }

    @Test
    public void skip_approvedMR() throws IOException, ExecutionException, InterruptedException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);
        testProject.setScm(new GitSCM(gitRepoUrl));
        QueueTaskFuture<?> future = testProject.scheduleBuild2(0, new ParametersAction(new StringParameterValue("gitlabTargetBranch", "master")));
        future.get();

        executeMergeRequestAction(testProject, getJson("MergeRequestEvent_approvedMR.json"));

        assertFalse(wouldFire);
    }

    @Test
    public void skip_alreadyBuiltMR() throws IOException, ExecutionException, InterruptedException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);
        testProject.setScm(new GitSCM(gitRepoUrl));
        QueueTaskFuture<?> future = testProject.scheduleBuild2(0, new ParametersAction(new StringParameterValue("gitlabTargetBranch", "master")));
        future.get();

        executeMergeRequestAction(testProject, getJson("MergeRequestEvent_alreadyBuiltMR.json"));
        assertFalse(wouldFire);

    }

    @Test
    public void build_alreadyBuiltMR_differentTargetBranch() throws IOException, ExecutionException, InterruptedException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);
        testProject.setScm(new GitSCM(gitRepoUrl));
        QueueTaskFuture<?> future = testProject.scheduleBuild2(0, new GitLabWebHookCause(causeData()
                .withActionType(CauseData.ActionType.MERGE)
                .withSourceProjectId(1)
                .withTargetProjectId(1)
                .withBranch("feature")
                .withSourceBranch("feature")
                .withUserName("")
                .withSourceRepoHomepage("https://gitlab.org/test")
                .withSourceRepoName("test")
                .withSourceNamespace("test-namespace")
                .withSourceRepoUrl("git@gitlab.org:test.git")
                .withSourceRepoSshUrl("git@gitlab.org:test.git")
                .withSourceRepoHttpUrl("https://gitlab.org/test.git")
                .withMergeRequestTitle("Test")
                .withMergeRequestId(1)
                .withMergeRequestIid(1)
                .withTargetBranch("master")
                .withTargetRepoName("test")
                .withTargetNamespace("test-namespace")
                .withTargetRepoSshUrl("git@gitlab.org:test.git")
                .withTargetRepoHttpUrl("https://gitlab.org/test.git")
                .withTriggeredByUser("test")
                .withLastCommit("123")
                .withTargetProjectUrl("https://gitlab.org/test")
                .build()));
        future.get();

        executeMergeRequestAction(testProject, getJson("MergeRequestEvent_alreadyBuiltMR.json"));

        assertTrue(wouldFire);
    }

    private String getJson(String name) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(name)).replace("${commitSha1}", commitSha1);
    }
}
