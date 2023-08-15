package com.dabsquared.gitlabjenkins.webhook.build;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.Queue;
import hudson.model.StringParameterValue;
import hudson.model.queue.QueueListener;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.git.GitSCM;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import javax.servlet.ServletException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.gitlab4j.api.models.Assignee;
import org.gitlab4j.api.models.Author;
import org.gitlab4j.api.models.User;
import org.gitlab4j.api.webhook.ChangeContainer;
import org.gitlab4j.api.webhook.EventCommit;
import org.gitlab4j.api.webhook.EventProject;
import org.gitlab4j.api.webhook.MergeRequestChanges;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.MergeRequestEvent.ObjectAttributes;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
    private MergeRequestEvent mergeRequestEvent = new MergeRequestEvent();

    @BeforeClass
    public static void addQueueListener() {
        QueueListener ql = new QueueListener() {
            @Override
            public void onEnterWaiting(Queue.WaitingItem wi) {
                System.out.println("Got " + wi + " : " + wi.getCausesDescription());
                wouldFire = true;
            }

            @Override
            public void onEnterBuildable(Queue.BuildableItem bi) {
                System.out.println("Is buildable: " + bi.getCausesDescription());
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

        mergeRequestEvent.setObjectKind("merge_request");
        User user = new User();
        user.setName("Administrator");
        user.setUsername("root");
        user.setAvatarUrl("http://www.gravatar.com/avatar/e32bd13e2add097461cb96824b7a829c?s=80\u0026d=identicon");
        mergeRequestEvent.setUser(user);
        ObjectAttributes objectAttributes = new ObjectAttributes();
        objectAttributes.setId(99L);
        objectAttributes.setTargetBranch("master");
        objectAttributes.setSourceBranch("ms-viewport");
        objectAttributes.setSourceProjectId(14L);
        objectAttributes.setAuthorId(51L);
        objectAttributes.setAssigneeId(6L);
        objectAttributes.setTitle("MS-Viewport");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        objectAttributes.setCreatedAt(dateFormat.parse("2013-12-03T17:23:34.123Z"));
        objectAttributes.setUpdatedAt(dateFormat.parse("2013-12-03T17:23:34.123Z"));
        objectAttributes.setStCommits(null);
        objectAttributes.setStDiffs(null);
        objectAttributes.setMilestoneId(null);
        objectAttributes.setState("opened");
        objectAttributes.setMergeStatus("unchecked");
        objectAttributes.setTargetProjectId(14L);
        objectAttributes.setIid(1L);
        objectAttributes.setDescription("");
        objectAttributes.setUrl("http://example.com/gitlab-org/gitlab-test/merge_requests/1#note_1244");
        EventProject sourceortargetproject = new EventProject();
        sourceortargetproject.setName("Awesome Project");
        sourceortargetproject.setDescription("Aut reprehenderit ut est.");
        sourceortargetproject.setWebUrl("http://example.com/awesome_space/awesome_project");
        sourceortargetproject.setAvatarUrl(null);
        sourceortargetproject.setGitSshUrl("git@example.com:awesome_space/awesome_project.git");
        sourceortargetproject.setGitHttpUrl("http://example.com/awesome_space/awesome_project.git");
        sourceortargetproject.setNamespace("Awesome Space");
        // sourceortargetproject.setVisibilityLevel(Visibility.PUBLIC);
        sourceortargetproject.setPathWithNamespace("awesome_space/awesome_project");
        sourceortargetproject.setDefaultBranch("master");
        sourceortargetproject.setHomepage("http://example.com/awesome_space/awesome_project");
        sourceortargetproject.setUrl("http://example.com/awesome_space/awesome_project.git");
        sourceortargetproject.setSshUrl("git@example.com:awesome_space/awesome_project.git");
        sourceortargetproject.setHttpUrl("http://example.com/awesome_space/awesome_project.git");
        objectAttributes.setSource(sourceortargetproject);
        objectAttributes.setTarget(sourceortargetproject);
        EventCommit lastCommit = new EventCommit();
        lastCommit.setId("da1560886d4f094c3e6c9ef40349f7d38b5d27d7");
        lastCommit.setMessage("fixed readme");
        SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        lastCommit.setTimestamp(dateFormat2.parse("2012-01-03T23:36:29+02:00"));
        lastCommit.setUrl(
                "http://example.com/awesome_space/awesome_project/commits/da1560886d4f094c3e6c9ef40349f7d38b5d27d7");
        Author commitAuthor = new Author();
        commitAuthor.setName("GitLab dev user");
        commitAuthor.setEmail("gitlabdev@dv6700.(none)");
        lastCommit.setAuthor(commitAuthor);
        objectAttributes.setLastCommit(lastCommit);
        objectAttributes.setWorkInProgress(false);
        objectAttributes.setAction("open");
        Assignee assignee2 = new Assignee();
        assignee2.setName("User1");
        assignee2.setUsername("user1");
        assignee2.setAvatarUrl("http://www.gravatar.com/avatar/e64c7d89f26bd1972efa854d13d7dd61?s=40\u0026d=identicon");
        objectAttributes.setAssignee(assignee2);
        mergeRequestEvent.setObjectAttributes(objectAttributes);
    }

    @Test
    public void build() throws IOException {
        GitLabPushTrigger mockTrigger = mock(GitLabPushTrigger.class);
        try {
            FreeStyleProject testProject = jenkins.createFreeStyleProject();
            testProject.addTrigger(mockTrigger);
            executeMergeRequestAction(testProject, mergeRequestEvent);
        } finally {
            ArgumentCaptor<MergeRequestEvent> pushHookArgumentCaptor = ArgumentCaptor.forClass(MergeRequestEvent.class);
            verify(mockTrigger).onPost(pushHookArgumentCaptor.capture());
            assertThat(pushHookArgumentCaptor.getValue().getProject(), is(notNullValue()));
            assertThat(pushHookArgumentCaptor.getValue().getProject().getWebUrl(), is(notNullValue()));
        }
    }

    private void executeMergeRequestAction(FreeStyleProject testProject, MergeRequestEvent event) throws IOException {
        try {
            wouldFire = false;

            trigger.start(testProject, false);

            new MergeRequestBuildAction(testProject, event, null).execute(response);
        } catch (HttpResponses.HttpResponseException hre) {
            // Test for OK status of a response.
            try {
                hre.generateResponse(null, response, null);
                verify(response, atLeastOnce()).setStatus(200);
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

        MergeRequestEvent mergeRequestEvent_closedMR = mergeRequestEvent;
        mergeRequestEvent_closedMR.getObjectAttributes().setState("closed");
        executeMergeRequestAction(testProject, mergeRequestEvent_closedMR);
        assertFalse(wouldFire);
    }

    @Test
    public void skip_approvedMR() throws IOException, ExecutionException, InterruptedException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);
        testProject.setScm(new GitSCM(gitRepoUrl));

        MergeRequestEvent mergeRequestEvent_approvedMR = mergeRequestEvent;
        mergeRequestEvent_approvedMR.getObjectAttributes().setAction("approved");
        executeMergeRequestAction(testProject, mergeRequestEvent_approvedMR);

        assertFalse(wouldFire);
    }

    @Test
    public void skip_alreadyBuiltMR() throws Exception {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);
        testProject.setScm(new GitSCM(gitRepoUrl));
        MergeRequestEvent mergeRequestEvent_alreadyBuiltMR = mergeRequestEvent;
        mergeRequestEvent_alreadyBuiltMR.getObjectAttributes().setAction("reopen");
        MergeRequestEvent mergeRequestEvent_alreadyBuiltMR_initialBuild = mergeRequestEvent;
        mergeRequestEvent_alreadyBuiltMR_initialBuild
                .getObjectAttributes()
                .getLastCommit()
                .setId("${commitSha1}");
        executeMergeRequestAction(testProject, mergeRequestEvent_alreadyBuiltMR_initialBuild);
        jenkins.waitUntilNoActivity();
        executeMergeRequestAction(testProject, mergeRequestEvent_alreadyBuiltMR);
        assertFalse(wouldFire);
    }

    @Test
    public void build_acceptedMr() throws IOException, ExecutionException, InterruptedException, ParseException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        trigger.setTriggerOnAcceptedMergeRequest(true);
        trigger.setTriggerOnMergeRequest(false);
        testProject.addTrigger(trigger);
        testProject.setScm(new GitSCM(gitRepoUrl));
        QueueTaskFuture<?> future = testProject.scheduleBuild2(
                0, new ParametersAction(new StringParameterValue("gitlabTargetBranch", "master")));
        future.get();

        MergeRequestEvent mergeRequestEvent_merged = mergeRequestEvent;
        mergeRequestEvent_merged.getObjectAttributes().setAction("merged");
        mergeRequestEvent_merged.getObjectAttributes().getLastCommit().setId("${commitSha1}");
        MergeRequestChanges mergeRequestChanges = new MergeRequestChanges();
        ChangeContainer<String> state = new ChangeContainer<>();
        state.setPrevious("locked");
        state.setCurrent("merged");
        mergeRequestChanges.setState(state);
        ChangeContainer<Date> updatedAt = new ChangeContainer<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        updatedAt.setPrevious(dateFormat.parse("2018-03-28 15:36:42 UTC"));
        updatedAt.setCurrent(dateFormat.parse("2018-03-28 15:36:42 UTC"));
        mergeRequestChanges.setUpdatedAt(updatedAt);
        ChangeContainer<Integer> total_time_spent = new ChangeContainer<>();
        total_time_spent.setPrevious(null);
        total_time_spent.setCurrent(0);
        mergeRequestChanges.setTotalTimeSpent(total_time_spent);
        mergeRequestEvent_merged.setChanges(mergeRequestChanges);
        executeMergeRequestAction(testProject, mergeRequestEvent_merged);
        assertTrue(wouldFire);
    }

    @Test
    public void build_alreadyBuiltMR_differentTargetBranch()
            throws IOException, ExecutionException, InterruptedException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);
        testProject.setScm(new GitSCM(gitRepoUrl));
        QueueTaskFuture<?> future = testProject.scheduleBuild2(
                0,
                new GitLabWebHookCause(causeData()
                        .withActionType(CauseData.ActionType.MERGE)
                        .withSourceProjectId(1L)
                        .withTargetProjectId(1L)
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
                        .withMergeRequestId(1L)
                        .withMergeRequestIid(1L)
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

        MergeRequestEvent mergeRequestEvent_alreadyBuiltMR_differentTargetBranch = mergeRequestEvent;
        mergeRequestEvent_alreadyBuiltMR_differentTargetBranch
                .getObjectAttributes()
                .setTargetBranch("develop");
        mergeRequestEvent_alreadyBuiltMR_differentTargetBranch
                .getObjectAttributes()
                .getLastCommit()
                .setId("${commitSha1}");
        mergeRequestEvent_alreadyBuiltMR_differentTargetBranch
                .getObjectAttributes()
                .setAction("update");
        executeMergeRequestAction(testProject, mergeRequestEvent_alreadyBuiltMR_differentTargetBranch);

        assertTrue(wouldFire);
    }
}
