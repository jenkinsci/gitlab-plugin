package com.dabsquared.gitlabjenkins.trigger.handler;

import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestObjectAttributesBuilder.mergeRequestObjectAttributes;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.UserBuilder.user;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
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
import hudson.model.Executor;
import hudson.model.FreeStyleProject;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Project;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.util.RunList;
import java.io.File;
import java.util.Arrays;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@WithJenkins
@ExtendWith(MockitoExtension.class)
class PendingBuildsHandlerTest {

    private static final String GITLAB_BUILD_NAME = "Jenkins";

    private static JenkinsRule jenkins;

    @Mock
    private GitLabClient gitLabClient;

    @Mock
    private GitLabConnectionProperty gitLabConnectionProperty;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @BeforeEach
    void setUp() {
        when(gitLabConnectionProperty.getClient()).thenReturn(gitLabClient);
    }

    @AfterEach
    void tearDown() {
        Queue queue = jenkins.getInstance().getQueue();
        for (Queue.Item item : queue.getItems()) {
            queue.cancel(item);
        }
    }

    @Test
    void projectCanBeConfiguredToSendPendingBuildStatusWhenTriggered() throws Exception {
        Project project =
                freestyleProject("freestyleProject1", new GitLabCommitStatusPublisher(GITLAB_BUILD_NAME, false));

        GitLabPushTrigger gitLabPushTrigger = gitLabPushTrigger(project);

        gitLabPushTrigger.onPost(pushHook(1, "branch1", "commit1"));

        verify(gitLabClient)
                .changeBuildStatus(
                        eq(1),
                        eq("commit1"),
                        eq(BuildState.pending),
                        eq("branch1"),
                        eq(GITLAB_BUILD_NAME),
                        contains("/freestyleProject1/"),
                        eq(BuildState.pending.name()));
        verifyNoMoreInteractions(gitLabClient);
    }

    @Test
    void workflowJobCanConfiguredToSendToPendingBuildStatusWhenTriggered() throws Exception {
        WorkflowJob workflowJob = workflowJob();

        GitLabPushTrigger gitLabPushTrigger = gitLabPushTrigger(workflowJob);
        gitLabPushTrigger.setPendingBuildName(GITLAB_BUILD_NAME);

        gitLabPushTrigger.onPost(mergeRequestHook(1, "branch1", "commit1"));

        verify(gitLabClient)
                .changeBuildStatus(
                        eq(1),
                        eq("commit1"),
                        eq(BuildState.pending),
                        eq("branch1"),
                        eq(GITLAB_BUILD_NAME),
                        contains("/workflowJob/"),
                        eq(BuildState.pending.name()));
        verifyNoMoreInteractions(gitLabClient);
    }

    @Test
    void queuedMergeRequestBuildsCanBeCancelledOnMergeRequestUpdate() throws Exception {
        Project project = freestyleProject("project1", new GitLabCommitStatusPublisher(GITLAB_BUILD_NAME, false));

        GitLabPushTrigger gitLabPushTrigger = gitLabPushTrigger(project);
        gitLabPushTrigger.setCancelPendingBuildsOnUpdate(true);

        assertThat(jenkins.getInstance().getQueue().getItems().length, is(0));

        gitLabPushTrigger.onPost(mergeRequestHook(1, "sourceBranch", "commit1")); // Will be cancelled
        gitLabPushTrigger.onPost(mergeRequestHook(1, "sourceBranch", "commit2")); // Will be cancelled
        gitLabPushTrigger.onPost(mergeRequestHook(1, "sourceBranch", "commit3"));
        gitLabPushTrigger.onPost(mergeRequestHook(1, "anotherBranch", "commit4"));
        gitLabPushTrigger.onPost(mergeRequestHook(2, "sourceBranch", "commit5"));

        verify(gitLabClient)
                .changeBuildStatus(
                        eq(1),
                        eq("commit1"),
                        eq(BuildState.canceled),
                        eq("sourceBranch"),
                        eq("Jenkins"),
                        contains("project1"),
                        eq(BuildState.canceled.name()));
        verify(gitLabClient)
                .changeBuildStatus(
                        eq(1),
                        eq("commit2"),
                        eq(BuildState.canceled),
                        eq("sourceBranch"),
                        eq("Jenkins"),
                        contains("project1"),
                        eq(BuildState.canceled.name()));

        assertThat(jenkins.getInstance().getQueue().getItems().length, is(3));
    }

    @Test
    void cancelRunningBuildsAbortsOnlyBuildsMatchingSourceProjectIdAndSourceBranch() {
        Job<?, ?> job = mock(Job.class);
        when(job.getName()).thenReturn("job1");
        when(job.getFullName()).thenReturn("job1");

        // Matching: same source project, same source branch — should be aborted.
        Executor matchExec1 = mock(Executor.class);
        Run<?, ?> match1 = runningBuildWithCause(matchExec1, 1, "feature-branch", "commit-a");

        Executor matchExec2 = mock(Executor.class);
        Run<?, ?> match2 = runningBuildWithCause(matchExec2, 1, "feature-branch", "commit-b");

        // Non-matching: same project, different branch.
        Executor otherBranchExec = mock(Executor.class);
        Run<?, ?> otherBranch = runningBuildWithCause(otherBranchExec, 1, "another-branch", "commit-c");

        // Non-matching: different project, same branch.
        Executor otherProjectExec = mock(Executor.class);
        Run<?, ?> otherProject = runningBuildWithCause(otherProjectExec, 2, "feature-branch", "commit-d");

        // Non-matching: already finished.
        Executor finishedExec = mock(Executor.class);
        Run<?, ?> finished = mock(Run.class);
        when(finished.isBuilding()).thenReturn(false);

        // Non-matching: building but has no GitLab cause (e.g. triggered manually).
        Run<?, ?> manualBuild = mock(Run.class);
        when(manualBuild.isBuilding()).thenReturn(true);
        when(manualBuild.getCause(GitLabWebHookCause.class)).thenReturn(null);

        RunList<Run<?, ?>> runs =
                RunList.fromRuns(Arrays.asList(match1, match2, otherBranch, otherProject, finished, manualBuild));
        doReturn(runs).when(job).getBuilds();

        new PendingBuildsHandler().cancelRunningBuilds(job, 1, "feature-branch");

        verify(matchExec1).interrupt(eq(Result.ABORTED), any(PendingBuildsHandler.SupersededByMergeRequestUpdate.class));
        verify(matchExec2).interrupt(eq(Result.ABORTED), any(PendingBuildsHandler.SupersededByMergeRequestUpdate.class));
        verify(otherBranchExec, never()).interrupt(any(), any());
        verify(otherProjectExec, never()).interrupt(any(), any());
        verify(finishedExec, never()).interrupt(any(), any());
    }

    private static Run<?, ?> runningBuildWithCause(
            Executor executor, int sourceProjectId, String sourceBranch, String commit) {
        Run<?, ?> run = mock(Run.class);
        when(run.isBuilding()).thenReturn(true);
        when(run.getExecutor()).thenReturn(executor);

        CauseData causeData = mock(CauseData.class);
        when(causeData.getSourceProjectId()).thenReturn(sourceProjectId);
        when(causeData.getSourceBranch()).thenReturn(sourceBranch);
        when(causeData.getLastCommit()).thenReturn(commit);

        GitLabWebHookCause cause = mock(GitLabWebHookCause.class);
        when(cause.getData()).thenReturn(causeData);
        when(run.getCause(GitLabWebHookCause.class)).thenReturn(cause);
        return run;
    }

    private GitLabPushTrigger gitLabPushTrigger(Project project) throws Exception {
        GitLabPushTrigger gitLabPushTrigger = gitLabPushTrigger();
        project.addTrigger(gitLabPushTrigger);
        gitLabPushTrigger.start(project, true);
        return gitLabPushTrigger;
    }

    private GitLabPushTrigger gitLabPushTrigger(WorkflowJob workflowJob) throws Exception {
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

    private PushHook pushHook(int projectId, String branch, String commitId) {
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
            throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject(name);
        project.setQuietPeriod(5000);
        project.getPublishersList().add(gitLabCommitStatusPublisher);
        project.addProperty(gitLabConnectionProperty);
        return project;
    }

    private WorkflowJob workflowJob() throws Exception {
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
