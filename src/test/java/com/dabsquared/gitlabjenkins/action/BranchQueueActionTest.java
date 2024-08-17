package com.dabsquared.gitlabjenkins.action;

import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestObjectAttributesBuilder.mergeRequestObjectAttributes;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.UserBuilder.user;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.Action;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestHookBuilder;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.ProjectBuilder;
import com.dabsquared.gitlabjenkins.publisher.GitLabCommitStatusPublisher;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.model.Queue;
import java.io.IOException;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BranchQueueActionTest {

    private static final String GITLAB_BUILD_NAME = "Jenkins";

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Mock
    private GitLabClient gitLabClient;

    @Mock
    private GitLabConnectionProperty gitLabConnectionProperty;

    @Before
    public void init() {
        when(gitLabConnectionProperty.getClient()).thenReturn(gitLabClient);
    }

    @After
    public void clearQueue() {
        Queue queue = jenkins.getInstance().getQueue();
        for (Queue.Item item : queue.getItems()) {
            queue.cancel(item);
        }
    }

    @Test
    public void queuedMergeRequestBuildsAreNotCancelledForDifferentSourceBranch() throws IOException {
        Project project = freestyleProject("project1", new GitLabCommitStatusPublisher(GITLAB_BUILD_NAME, false));

        GitLabPushTrigger gitLabPushTrigger = gitLabPushTrigger(project);
        gitLabPushTrigger.setCancelPendingBuildsOnUpdate(true);

        assertThat(jenkins.getInstance().getQueue().getItems().length, is(0));

        gitLabPushTrigger.onPost(mergeRequestHook(1, "sourceBranch", "commit1"));
        gitLabPushTrigger.onPost(
                mergeRequestHook(1, "anotherBranch", "commit1")); // Same commit different source branch

        assertThat(jenkins.getInstance().getQueue().getItems().length, is(2));
    }

    @Test
    public void queuedMergeRequestBuildsAreCancelledForSameBranches() throws IOException {
        Project project = freestyleProject("project3", new GitLabCommitStatusPublisher(GITLAB_BUILD_NAME, false));

        GitLabPushTrigger gitLabPushTrigger = gitLabPushTrigger(project);
        gitLabPushTrigger.setCancelPendingBuildsOnUpdate(true);

        assertThat(jenkins.getInstance().getQueue().getItems().length, is(0));

        gitLabPushTrigger.onPost(mergeRequestHook(1, "sourceBranch", "commit1"));
        gitLabPushTrigger.onPost(mergeRequestHook(1, "sourceBranch", "commit1")); // Same commit and branches

        assertThat(jenkins.getInstance().getQueue().getItems().length, is(1));
    }

    @Test
    public void testNullSourceBranch() {
        BranchQueueAction action1 = new BranchQueueAction(null);
        BranchQueueAction action2 = new BranchQueueAction("sourceBranch");

        boolean resultOfScheduleNull = action1.shouldSchedule(Arrays.asList(action2));
        boolean resultOfQueuedNull = action2.shouldSchedule(Arrays.asList(action1));

        assertTrue(resultOfScheduleNull);
        assertTrue(resultOfQueuedNull);
    }

    private GitLabPushTrigger gitLabPushTrigger(Project project) throws IOException {
        GitLabPushTrigger gitLabPushTrigger = gitLabPushTrigger();
        project.addTrigger(gitLabPushTrigger);
        gitLabPushTrigger.start(project, true);
        return gitLabPushTrigger;
    }

    private GitLabPushTrigger gitLabPushTrigger() {
        GitLabPushTrigger gitLabPushTrigger = new GitLabPushTrigger();
        gitLabPushTrigger.setTriggerOnPush(true);
        gitLabPushTrigger.setTriggerOnMergeRequest(true);
        gitLabPushTrigger.setPendingBuildName(GITLAB_BUILD_NAME);
        gitLabPushTrigger.setBranchFilterType(BranchFilterType.NameBasedFilter);
        gitLabPushTrigger.setBranchFilterName("");
        return gitLabPushTrigger;
    }

    private MergeRequestHook mergeRequestHook(int projectId, String sourceBranch, String commitId) {
        return MergeRequestHookBuilder.mergeRequestHook()
                .withObjectAttributes(mergeRequestObjectAttributes()
                        .withAction(Action.update)
                        .withState(State.updated)
                        .withIid(1)
                        .withTitle("test")
                        .withTargetProjectId(1)
                        .withTargetBranch("targetBranch")
                        .withSourceBranch(sourceBranch)
                        .withSourceProjectId(projectId)
                        .withLastCommit(
                                commit().withAuthor(user().withName("author").build())
                                        .withId(commitId)
                                        .build())
                        .withSource(ProjectBuilder.project()
                                .withName("test")
                                .withNamespace("test-namespace")
                                .withHomepage("https://gitlab.org/test")
                                .withUrl("git@gitlab.org:test.git")
                                .withSshUrl("git@gitlab.org:test.git")
                                .withHttpUrl("https://gitlab.org/test.git")
                                .build())
                        .withTarget(ProjectBuilder.project()
                                .withName("test")
                                .withNamespace("test-namespace")
                                .withHomepage("https://gitlab.org/test")
                                .withUrl("git@gitlab.org:test.git")
                                .withSshUrl("git@gitlab.org:test.git")
                                .withHttpUrl("https://gitlab.org/test.git")
                                .build())
                        .build())
                .withProject(ProjectBuilder.project()
                        .withWebUrl("https://gitlab.org/test.git")
                        .build())
                .build();
    }

    private Project freestyleProject(String name, GitLabCommitStatusPublisher gitLabCommitStatusPublisher)
            throws IOException {
        FreeStyleProject project = jenkins.createFreeStyleProject(name);
        project.setQuietPeriod(5000);
        project.getPublishersList().add(gitLabCommitStatusPublisher);
        project.addProperty(gitLabConnectionProperty);
        return project;
    }
}
