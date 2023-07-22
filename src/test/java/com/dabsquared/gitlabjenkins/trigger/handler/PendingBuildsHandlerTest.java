package com.dabsquared.gitlabjenkins.trigger.handler;

import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestObjectAttributesBuilder.mergeRequestObjectAttributes;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.UserBuilder.user;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
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
import java.util.Collections;
import org.gitlab4j.api.CommitsApi;
import org.gitlab4j.api.Constants.ActionType;
import org.gitlab4j.api.Constants.CommitBuildState;
import org.gitlab4j.api.Constants.MergeRequestState;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.Author;
import org.gitlab4j.api.models.CommitStatus;
import org.gitlab4j.api.models.Repository;
import org.gitlab4j.api.models.User;
import org.gitlab4j.api.webhook.EventCommit;
import org.gitlab4j.api.webhook.EventProject;
import org.gitlab4j.api.webhook.EventRepository;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.MergeRequestEvent.ObjectAttributes;
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
        Project<?, ?> project =
                freestyleProject("freestyleProject1", new GitLabCommitStatusPublisher(GITLAB_BUILD_NAME, false));

        GitLabPushTrigger gitLabPushTrigger = gitLabPushTrigger(project);

        gitLabPushTrigger.onPost(pushEvent(1L, "branch1", "commit1"));
        CommitStatus status = new CommitStatus();
        status.withRef("branch1")
                .withName(GITLAB_BUILD_NAME)
                .withDescription(CommitBuildState.PENDING.name())
                .withCoverage(null)
                .withTargetUrl(jenkins.getURL() + "job/freestyleProject1/display/redirect");
        when(gitLabClient.getCommitsApi()).thenReturn(commitsApi);
        verify(commitsApi).addCommitStatus(eq(1L), eq("commit1"), eq(CommitBuildState.PENDING), refEq(status));
    }

    @Test
    public void workflowJobCanConfiguredToSendToPendingBuildStatusWhenTriggered() throws Exception {
        WorkflowJob workflowJob = workflowJob();

        GitLabPushTrigger gitLabPushTrigger = gitLabPushTrigger(workflowJob);
        gitLabPushTrigger.setPendingBuildName(GITLAB_BUILD_NAME);

        gitLabPushTrigger.onPost(mergeRequestEvent(1L, "branch1", "commit1"));

        CommitStatus status = new CommitStatus();
        status.withRef("branch1")
                .withName(GITLAB_BUILD_NAME)
                .withDescription(CommitBuildState.PENDING.name())
                .withCoverage(null)
                .withTargetUrl(jenkins.getURL() + "nullprefix/workflowJob/display/redirect");

        when(gitLabClient.getCommitsApi()).thenReturn(commitsApi);
        verify(commitsApi).addCommitStatus(eq(1L), eq("commit1"), eq(CommitBuildState.PENDING), refEq(status));
    }

    @Test
    public void queuedMergeRequestBuildsCanBeCancelledOnMergeRequestUpdate() throws Exception {
        Project<?, ?> project = freestyleProject("project1", new GitLabCommitStatusPublisher(GITLAB_BUILD_NAME, false));

        GitLabPushTrigger gitLabPushTrigger = gitLabPushTrigger(project);
        gitLabPushTrigger.setCancelPendingBuildsOnUpdate(true);

        assertThat(jenkins.getInstance().getQueue().getItems().length, is(0));

        gitLabPushTrigger.onPost(mergeRequestEvent(1L, "sourceBranch", "commit1")); // Will be cancelled
        gitLabPushTrigger.onPost(mergeRequestEvent(1L, "sourceBranch", "commit2")); // Will be cancelled
        gitLabPushTrigger.onPost(mergeRequestEvent(1L, "sourceBranch", "commit3"));
        gitLabPushTrigger.onPost(mergeRequestEvent(1L, "anotherBranch", "commit4"));
        gitLabPushTrigger.onPost(mergeRequestEvent(2L, "sourceBranch", "commit5"));

        CommitStatus status = new CommitStatus();
        status.withRef("sourceBranch")
                .withName("Jenkins")
                .withDescription(CommitBuildState.CANCELED.name())
                .withCoverage(null)
                .withTargetUrl(jenkins.getURL() + "/job/project1/display/redirect");

        when(gitLabClient.getCommitsApi()).thenReturn(commitsApi);
        verify(commitsApi).addCommitStatus(eq(1L), eq("commit1"), eq(CommitBuildState.CANCELED), refEq(status));
        verify(commitsApi).addCommitStatus(eq(1L), eq("commit2"), eq(CommitBuildState.CANCELED), refEq(status));

        assertThat(jenkins.getInstance().getQueue().getItems().length, is(3));
    }

    private GitLabPushTrigger gitLabPushTrigger(Project<?, ?> project) throws IOException {
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

    private MergeRequestEvent mergeRequestEvent(Long projectId, String branch, String commitId) {
        ObjectAttributes objectAttributes = new ObjectAttributes();
        objectAttributes.setIid(1L);
        objectAttributes.setAction((ActionType.UPDATED).toString());
        objectAttributes.setState((MergeRequestState.OPENED).toString());
        objectAttributes.setTitle("test");
        objectAttributes.setTargetProjectId(1L);
        objectAttributes.setSourceProjectId(1L);
        objectAttributes.setSourceBranch("feature");
        objectAttributes.setTargetBranch("master");
        EventCommit lastCommit = new EventCommit();
        Author author = new Author();
        author.setName("test");
        lastCommit.setAuthor(author);
        lastCommit.setId("testid");
        objectAttributes.setLastCommit(lastCommit);
        EventProject eventProject = new EventProject();
        eventProject.setName("test");
        eventProject.setNamespace("test-namespace");
        eventProject.setHomepage("https://gitlab.org/test");
        eventProject.setUrl("git@gitlab.org:test.git");
        eventProject.setSshUrl("git@gitlab.org:test.git");
        eventProject.setHttpUrl("https://gitlab.org/test.git");
        objectAttributes.setSource(eventProject);
        objectAttributes.setTarget(eventProject);
        EventProject project = new EventProject();
        project.setWebUrl("https://gitlab.org/test.git");
        MergeRequestEvent mergeRequestEvent = new MergeRequestEvent();
        mergeRequestEvent.setObjectAttributes(objectAttributes);
        mergeRequestEvent.setProject(project);
        return mergeRequestEvent;
    }

    private PushEvent pushEvent(Long projectId, String branch, String commitId) {

        EventRepository repository = new EventRepository();
        repository.setName("repository");
        repository.setGit_http_url("httpUrl");
        repository.setGit_ssh_url("sshUrl");

        EventProject project = new EventProject();
        project.setNamespace("namespace");

        EventCommit commit = new EventCommit();
        commit.setId(commitId);
        Author author = new Author();
        author.setUsername("username");
        commit.setAuthor(author);

        PushEvent pushEvent = new PushEvent();
        pushEvent.setProjectId(projectId);
        pushEvent.setRef(branch);
        pushEvent.setAfter(commitId);
        pushEvent.setRepository(repository);
        pushEvent.setProject(project);
        pushEvent.setCommits(Collections.singletonList(commit));
        pushEvent.setObjectKind("push");
        pushEvent.setUserName("username");
        return pushEvent;
    }

    private Project<?, ?> freestyleProject(String name, GitLabCommitStatusPublisher gitLabCommitStatusPublisher)
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
