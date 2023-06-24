package com.dabsquared.gitlabjenkins.trigger.handler;

import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestObjectAttributesBuilder.mergeRequestObjectAttributes;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.UserBuilder.user;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.Action;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.Repository;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.User;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.CommitBuilder;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestHookBuilder;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.ProjectBuilder;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.PushHookBuilder;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.RepositoryBuilder;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.UserBuilder;
import com.dabsquared.gitlabjenkins.publisher.GitLabCommitStatusPublisher;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import hudson.model.FreeStyleProject;
import hudson.model.ItemGroup;
import hudson.model.Project;
import hudson.model.Queue;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.gitlab4j.api.CommitsApi;
import org.gitlab4j.api.Constants.CommitBuildState;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.CommitStatus;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PendingBuildsHandlerTest {

    private static final String GITLAB_BUILD_NAME = "Jenkins";

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Mock
    private GitLabApi gitLabClient;

    @Mock
    private CommitsApi commitsApi;

    @Mock
    private GitLabConnectionProperty gitLabConnectionProperty;

    @Before
    public void init() {
        when(gitLabConnectionProperty.getClient()).thenReturn(gitLabClient);
        when(gitLabClient.getCommitsApi()).thenReturn(commitsApi);
    }

    @After
    public void clearQueue() {
        Queue queue = jenkins.getInstance().getQueue();
        for (Queue.Item item : queue.getItems()) {
            queue.cancel(item);
        }
    }

    @Test
    public void projectCanBeConfiguredToSendPendingBuildStatusWhenTriggered() throws Exception {
        Project project =
                freestyleProject("freestyleProject1", new GitLabCommitStatusPublisher(GITLAB_BUILD_NAME, false));

        GitLabPushTrigger gitLabPushTrigger = gitLabPushTrigger(project);

        gitLabPushTrigger.onPost(pushHook(1L, "branch1", "commit1"));
        CommitStatus status = new CommitStatus();
        status.withRef("branch1")
                .withName(GITLAB_BUILD_NAME)
                .withDescription(CommitBuildState.PENDING.name())
                .withCoverage(null)
                .withTargetUrl("/freestyleProject1/");
        when(gitLabClient.getCommitsApi()).thenReturn(commitsApi);
        verify(commitsApi).addCommitStatus(1L, "commit1", CommitBuildState.PENDING, status);
        verifyNoMoreInteractions(gitLabClient);
    }

    @Test
    public void workflowJobCanConfiguredToSendToPendingBuildStatusWhenTriggered() throws Exception {
        WorkflowJob workflowJob = workflowJob();

        GitLabPushTrigger gitLabPushTrigger = gitLabPushTrigger(workflowJob);
        gitLabPushTrigger.setPendingBuildName(GITLAB_BUILD_NAME);

        gitLabPushTrigger.onPost(mergeRequestHook(1L, "branch1", "commit1"));

        CommitStatus status = new CommitStatus();
        status.withRef("branch1")
                .withName(GITLAB_BUILD_NAME)
                .withDescription(CommitBuildState.PENDING.name())
                .withCoverage(null)
                .withTargetUrl("/WorkflowJob/");

        when(gitLabClient.getCommitsApi()).thenReturn(commitsApi);
        verify(commitsApi).addCommitStatus(1L, "commit1", CommitBuildState.PENDING, status);
        verifyNoMoreInteractions(gitLabClient);
    }

    @Test
    public void queuedMergeRequestBuildsCanBeCancelledOnMergeRequestUpdate() throws Exception {
        Project project = freestyleProject("project1", new GitLabCommitStatusPublisher(GITLAB_BUILD_NAME, false));

        GitLabPushTrigger gitLabPushTrigger = gitLabPushTrigger(project);
        gitLabPushTrigger.setCancelPendingBuildsOnUpdate(true);

        assertThat(jenkins.getInstance().getQueue().getItems().length, is(0));

        gitLabPushTrigger.onPost(mergeRequestHook(1L, "sourceBranch", "commit1")); // Will be cancelled
        gitLabPushTrigger.onPost(mergeRequestHook(1L, "sourceBranch", "commit2")); // Will be cancelled
        gitLabPushTrigger.onPost(mergeRequestHook(1L, "sourceBranch", "commit3"));
        gitLabPushTrigger.onPost(mergeRequestHook(1L, "anotherBranch", "commit4"));
        gitLabPushTrigger.onPost(mergeRequestHook(2L, "sourceBranch", "commit5"));

        CommitStatus status = new CommitStatus();
        status.withRef("sourceBranch")
                .withName("Jenkins")
                .withDescription(CommitBuildState.CANCELED.name())
                .withCoverage(null)
                .withTargetUrl("project1");

        when(gitLabClient.getCommitsApi()).thenReturn(commitsApi);
        verify(commitsApi).addCommitStatus(1L, "commit1", CommitBuildState.CANCELED, status);
        verify(commitsApi).addCommitStatus(1L, "commit2", CommitBuildState.CANCELED, status);

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
        gitLabPushTrigger.setBranchFilterName("");
        return gitLabPushTrigger;
    }

    private MergeRequestHook mergeRequestHook(Long projectId, String branch, String commitId) {
        return MergeRequestHookBuilder.mergeRequestHook()
                .withObjectAttributes(mergeRequestObjectAttributes()
                        .withAction(Action.update)
                        .withState(State.updated)
                        .withIid(1L)
                        .withTitle("test")
                        .withTargetProjectId(1L)
                        .withTargetBranch("targetBranch")
                        .withSourceBranch(branch)
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

    private PushHook pushHook(Long projectId, String branch, String commitId) {
        User user = new UserBuilder().withName("username").build();

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
                .withCommits(Arrays.asList(
                        CommitBuilder.commit().withId(commitId).withAuthor(user).build()))
                .withRepository(repository)
                .withObjectKind("push")
                .withUserName("username")
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
