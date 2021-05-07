package com.dabsquared.gitlabjenkins.trigger.handler;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.*;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.*;
import com.dabsquared.gitlabjenkins.publisher.GitLabCommitStatusPublisher;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import com.dabsquared.gitlabjenkins.trigger.filter.UserNameFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.UserNameFilterType;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Project;
import hudson.model.Queue;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestObjectAttributesBuilder.mergeRequestObjectAttributes;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.UserBuilder.user;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PendingBuildsHandlerTest {

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
    public void projectCanBeConfiguredToSendPendingBuildStatusWhenTriggered() throws IOException {
        Project project = freestyleProject("freestyleProject1", new GitLabCommitStatusPublisher(GITLAB_BUILD_NAME, false));

        GitLabPushTrigger gitLabPushTrigger = gitLabPushTrigger(project);

        gitLabPushTrigger.onPost(pushHook(1, "branch1", "commit1"));

        verify(gitLabClient).changeBuildStatus(eq(1), eq("commit1"), eq(BuildState.pending), eq("branch1"), eq(GITLAB_BUILD_NAME),
            contains("/freestyleProject1/"), eq(BuildState.pending.name()));
        verifyNoMoreInteractions(gitLabClient);
    }

    @Test
    public void workflowJobCanConfiguredToSendToPendingBuildStatusWhenTriggered() throws IOException {
        WorkflowJob workflowJob = workflowJob();

        GitLabPushTrigger gitLabPushTrigger =  gitLabPushTrigger(workflowJob);
        gitLabPushTrigger.setPendingBuildName(GITLAB_BUILD_NAME);

        gitLabPushTrigger.onPost(mergeRequestHook(1, "branch1", "commit1"));

        verify(gitLabClient).changeBuildStatus(eq(1), eq("commit1"), eq(BuildState.pending), eq("branch1"), eq(GITLAB_BUILD_NAME),
            contains("/workflowJob/"), eq(BuildState.pending.name()));
        verifyNoMoreInteractions(gitLabClient);
    }

    @Test
    public void queuedMergeRequestBuildsCanBeCancelledOnMergeRequestUpdate() throws IOException {
        Project project = freestyleProject("project1", new GitLabCommitStatusPublisher(GITLAB_BUILD_NAME, false));

        GitLabPushTrigger gitLabPushTrigger = gitLabPushTrigger(project);
        gitLabPushTrigger.setCancelPendingBuildsOnUpdate(true);

        assertThat(jenkins.getInstance().getQueue().getItems().length, is(0));

        gitLabPushTrigger.onPost(mergeRequestHook(1, "sourceBranch", "commit1")); // Will be cancelled
        gitLabPushTrigger.onPost(mergeRequestHook(1, "sourceBranch", "commit2")); // Will be cancelled
        gitLabPushTrigger.onPost(mergeRequestHook(1, "sourceBranch", "commit3"));
        gitLabPushTrigger.onPost(mergeRequestHook(1, "anotherBranch", "commit4"));
        gitLabPushTrigger.onPost(mergeRequestHook(2, "sourceBranch", "commit5"));

        verify(gitLabClient).changeBuildStatus(eq(1), eq("commit1"), eq(BuildState.canceled), eq("sourceBranch"),
            eq("Jenkins"), contains("project1"), eq(BuildState.canceled.name()));
        verify(gitLabClient).changeBuildStatus(eq(1), eq("commit2"), eq(BuildState.canceled), eq("sourceBranch"),
            eq("Jenkins"), contains("project1"), eq(BuildState.canceled.name()));

        assertThat(jenkins.getInstance().getQueue().getItems().length, is(3));
    }

    private GitLabPushTrigger gitLabPushTrigger(Project project) throws IOException {
        GitLabPushTrigger gitLabPushTrigger = gitLabPushTrigger();
        project.addTrigger(gitLabPushTrigger);
        gitLabPushTrigger.start(project, true);
        return gitLabPushTrigger;
    }

    private GitLabPushTrigger gitLabPushTrigger(WorkflowJob workflowJob) throws IOException {
        GitLabPushTrigger gitLabPushTrigger = gitLabPushTrigger();
        workflowJob.addTrigger(gitLabPushTrigger);
        gitLabPushTrigger.start(workflowJob, true);
        return gitLabPushTrigger;
    }

    private GitLabPushTrigger gitLabPushTrigger() {
        GitLabPushTrigger gitLabPushTrigger = new GitLabPushTrigger();
        gitLabPushTrigger.setTriggerOnPush(true);
        gitLabPushTrigger.setTriggerOnMergeRequest(true);
        gitLabPushTrigger.setPendingBuildName(GITLAB_BUILD_NAME);
        gitLabPushTrigger.setBranchFilterType(BranchFilterType.NameBasedFilter);
        gitLabPushTrigger.setUserNameFilterType(UserNameFilterType.NameBasedFilter);
        gitLabPushTrigger.setBranchFilterName("");
        return gitLabPushTrigger;
    }

    private MergeRequestHook mergeRequestHook(int projectId, String branch, String commitId) {
        return MergeRequestHookBuilder.mergeRequestHook()
            .withObjectAttributes(mergeRequestObjectAttributes()
                .withAction(Action.update)
                .withState(State.updated)
                .withIid(1)
                .withTitle("test")
                .withTargetProjectId(1)
                .withTargetBranch("targetBranch")
                .withSourceBranch(branch)
                .withSourceProjectId(projectId)
                .withLastCommit(commit().withAuthor(user().withName("author").build()).withId(commitId).build())
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
                .build()
            )
            .build();
    }

    private PushHook pushHook(int projectId, String branch, String commitId) {
        User user = new UserBuilder()
            .withName("username")
            .build();

        Repository repository = new RepositoryBuilder()
            .withName("repository")
            .withGitSshUrl("sshUrl")
            .withGitHttpUrl("httpUrl")
            .build();

        return new PushHookBuilder()
            .withProjectId(projectId)
            .withRef(branch)
            .withAfter(commitId)
            .withRepository(new Repository())
            .withProject(ProjectBuilder.project().withNamespace("namespace").build())
            .withCommits(Arrays.asList(CommitBuilder.commit().withId(commitId).withAuthor(user).build()))
            .withRepository(repository)
            .withObjectKind("push")
            .withUserName("username")
            .build();
    }

    private Project freestyleProject(String name, GitLabCommitStatusPublisher gitLabCommitStatusPublisher) throws IOException {
        FreeStyleProject project = jenkins.createFreeStyleProject(name);
        project.setQuietPeriod(5000);
        project.getPublishersList().add(gitLabCommitStatusPublisher);
        project.addProperty(gitLabConnectionProperty);
        return project;
    }

    private WorkflowJob workflowJob() throws IOException {
        ItemGroup itemGroup = mock(ItemGroup.class);
        when(itemGroup.getFullName()).thenReturn("parent");
        when(itemGroup.getUrlChildPrefix()).thenReturn("prefix");

        WorkflowJob workflowJob = new WorkflowJob(itemGroup, "workflowJob");
        when(itemGroup.getRootDirFor(workflowJob)).thenReturn(new File("work"));

        workflowJob.addProperty(gitLabConnectionProperty);
        workflowJob.setQuietPeriod(5000);
        workflowJob.onCreatedFromScratch();
        return workflowJob;
    }
}
