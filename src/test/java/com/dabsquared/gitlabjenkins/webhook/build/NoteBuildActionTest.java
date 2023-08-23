package com.dabsquared.gitlabjenkins.webhook.build;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.git.GitSCM;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.gitlab4j.api.models.Assignee;
import org.gitlab4j.api.models.Author;
import org.gitlab4j.api.models.User;
import org.gitlab4j.api.webhook.EventCommit;
import org.gitlab4j.api.webhook.EventMergeRequest;
import org.gitlab4j.api.webhook.EventProject;
import org.gitlab4j.api.webhook.EventRepository;
import org.gitlab4j.api.webhook.NoteEvent;
import org.gitlab4j.api.webhook.NoteEvent.NoteableType;
import org.gitlab4j.api.webhook.NoteEvent.ObjectAttributes;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nikolay Ustinov
 */
@RunWith(MockitoJUnitRunner.class)
public class NoteBuildActionTest {

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private StaplerResponse response;

    @Mock
    private GitLabPushTrigger trigger;

    private String gitRepoUrl;
    private String commitSha1;
    private NoteEvent noteEvent;

    @Before
    public void setup() throws Exception {
        Git.init().setDirectory(tmp.getRoot()).call();
        tmp.newFile("test");
        Git git = Git.open(tmp.getRoot());
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setMessage("test").call();
        commitSha1 = commit.getId().getName();
        gitRepoUrl = tmp.getRoot().toURI().toString();

        noteEvent = new NoteEvent();
        noteEvent.setObjectKind("note");
        User user = new User();
        user.setName("Administrator");
        user.setUsername("root");
        user.setAvatarUrl("http://www.gravatar.com/avatar/e32bd13e2add097461cb96824b7a829c?s=80\u0026d=identicon");
        noteEvent = new NoteEvent();
        noteEvent.setUser(user);
        noteEvent.setProjectId(5L);
        EventProject project = new EventProject();
        project.setName("Gitlab Test");
        project.setDescription("Aut reprehenderit ut est.");
        project.setWebUrl("git@example.com:gitlab-org/gitlab-test.git");
        project.setAvatarUrl(null);
        project.setGitSshUrl("git@example.com:mike/diaspora.git");
        project.setGitHttpUrl("http://example.com/gitlab-org/gitlab-test.git");
        project.setNamespace("Gitlab Org");
        project.setPathWithNamespace("gitlab-org/gitlab-test");
        project.setDefaultBranch("master");
        project.setHomepage("http://example.com/gitlab-org/gitlab-test");
        project.setUrl("http://example.com/gitlab-org/gitlab-test.git");
        project.setSshUrl("git@example.com:gitlab-org/gitlab-test.git");
        project.setHttpUrl("http://example.com/gitlab-org/gitlab-test.git");
        noteEvent.setProject(project);
        EventRepository repository = new EventRepository();
        repository.setName("Gitlab Test");
        repository.setUrl("http://localhost/gitlab-org/gitlab-test.git");
        repository.setDescription("Aut reprehenderit ut est.");
        repository.setHomepage("http://example.com/gitlab-org/gitlab-test");
        noteEvent.setRepository(repository);
        ObjectAttributes objectAttributes = new ObjectAttributes();
        objectAttributes.setId(1244L);
        objectAttributes.setNote("This MR needs work.");
        objectAttributes.NoteableType(NoteableType.MERGE_REQUEST);
        objectAttributes.setAuthorId(1L);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        objectAttributes.setCreatedAt(dateFormat.parse("2015-05-17 18:21:36 UTC"));
        objectAttributes.setUpdatedAt(dateFormat.parse("2015-05-17 18:21:36 UTC"));
        objectAttributes.setProjectId(5L);
        objectAttributes.setAttachment(null);
        objectAttributes.setLineCode(null);
        objectAttributes.setCommitId("");
        objectAttributes.setSystem(false);
        objectAttributes.setStDiff(null);
        objectAttributes.setUrl("http://example.com/gitlab-org/gitlab-test/merge_requests/1#note_1244");
        noteEvent.setObjectAttributes(objectAttributes);
        EventMergeRequest mergeRequest = new EventMergeRequest();
        mergeRequest.setId(1L);
        mergeRequest.setTargetBranch("markdown");
        mergeRequest.setSourceBranch("master");
        mergeRequest.setSourceProjectId(5L);
        mergeRequest.setAuthorId(8L);
        Assignee assignee = new Assignee();
        assignee.setId(6L);
        mergeRequest.setAssignee(assignee);
        mergeRequest.setTitle("Tempora et eos debitis quae laborum et.");
        mergeRequest.setCreatedAt(dateFormat.parse("2015-03-01 20:12:53 UTC"));
        mergeRequest.setUpdatedAt(dateFormat.parse("2015-03-21 18:27:27 UTC"));
        mergeRequest.setMilestoneId(11L);
        mergeRequest.setState("opened");
        mergeRequest.setMergeStatus("cannot_be_merged");
        mergeRequest.setTargetProjectId(5L);
        mergeRequest.setIid(1L);
        mergeRequest.setDescription(
                "Et voluptas corrupti assumenda temporibus. Architecto cum animi eveniet amet asperiores. Vitae numquam voluptate est natus sit et ad id.");
        mergeRequest.setPosition(0);
        mergeRequest.setLockedAt(null);
        EventProject sourceortargetproject = new EventProject();
        sourceortargetproject.setName("Gitlab Test");
        sourceortargetproject.setDescription("Aut reprehenderit ut est.");
        sourceortargetproject.setWebUrl("git@example.com:gitlab-org/gitlab-test.git");
        sourceortargetproject.setAvatarUrl(null);
        sourceortargetproject.setGitSshUrl("git@example.com:mike/diaspora.git");
        sourceortargetproject.setGitHttpUrl("http://example.com/gitlab-org/gitlab-test.git");
        sourceortargetproject.setNamespace("Gitlab Org");
        sourceortargetproject.setPathWithNamespace("gitlab-org/gitlab-test");
        sourceortargetproject.setDefaultBranch("master");
        sourceortargetproject.setHomepage("http://example.com/gitlab-org/gitlab-test");
        sourceortargetproject.setUrl("http://example.com/gitlab-org/gitlab-test.git");
        sourceortargetproject.setSshUrl("git@example.com:gitlab-org/gitlab-test.git");
        sourceortargetproject.setHttpUrl("http://example.com/gitlab-org/gitlab-test.git");
        mergeRequest.setTarget(sourceortargetproject);
        mergeRequest.setSource(sourceortargetproject);
        EventCommit lastCommit = new EventCommit();
        lastCommit.setId("562e173be03b8ff2efb05345d12df18815438a4b");
        lastCommit.setMessage("Merge branch 'another-branch' into 'master'\n\nCheck in this test\n");
        SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        lastCommit.setTimestamp(dateFormat2.parse("2015-04-08T21: 00:25-07:00"));
        lastCommit.setUrl("http://example.com/gitlab-org/gitlab-test/commit/562e173be03b8ff2efb05345d12df18815438a4b");
        Author commitAuthor = new Author();
        commitAuthor.setName("John Smith");
        commitAuthor.setEmail("john@example.com");
        lastCommit.setAuthor(commitAuthor);
        mergeRequest.setLastCommit(lastCommit);
        mergeRequest.setWorkInProgress(false);
        Assignee assignee2 = new Assignee();
        assignee2.setName("User1");
        assignee2.setUsername("user1");
        assignee2.setAvatarUrl("http://www.gravatar.com/avatar/e64c7d89f26bd1972efa854d13d7dd61?s=40\u0026d=identicon");
        mergeRequest.setAssignee(assignee2);
        noteEvent.setMergeRequest(mergeRequest);
    }

    @Test
    public void build() throws IOException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);

        //        exception.expect(HttpResponses.HttpResponseException.class);

        new NoteBuildAction(testProject, noteEvent, null).execute(response);

        verify(trigger).onPost(any(NoteEvent.class));
    }

    @Test
    public void build_alreadyBuiltMR_alreadyBuiltMR() throws IOException, ExecutionException, InterruptedException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);
        testProject.setScm(new GitSCM(gitRepoUrl));
        QueueTaskFuture<?> future = testProject.scheduleBuild2(
                0, new ParametersAction(new StringParameterValue("gitlabTargetBranch", "master")));
        future.get();

        //        exception.expect(HttpResponses.HttpResponseException.class);

        NoteEvent noteEvent_alreadyBuiltMR = noteEvent;
        noteEvent_alreadyBuiltMR.getMergeRequest().getLastCommit().setId("${commitSha1}");
        new NoteBuildAction(testProject, noteEvent_alreadyBuiltMR, null).execute(response);

        verify(trigger).onPost(any(NoteEvent.class));
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
                        .withActionType(CauseData.ActionType.NOTE)
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

        //        exception.expect(HttpResponses.HttpResponseException.class);

        NoteEvent noteEvent_alreadyBuiltMR = noteEvent;
        noteEvent_alreadyBuiltMR.getMergeRequest().getLastCommit().setId("${commitSha1}");
        new NoteBuildAction(testProject, noteEvent_alreadyBuiltMR, null).execute(response);

        verify(trigger).onPost(any(NoteEvent.class));
    }
}
