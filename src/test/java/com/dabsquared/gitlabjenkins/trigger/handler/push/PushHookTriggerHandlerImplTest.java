package com.dabsquared.gitlabjenkins.trigger.handler.push;

import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.ProjectBuilder.project;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.PushHookBuilder.pushHook;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.RepositoryBuilder.repository;
import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterFactory.newBranchFilter;
import static com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilterFactory.newMergeRequestLabelFilter;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.PushHookBuilder;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;

import hudson.Functions;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.GitSCM;
import hudson.util.OneShotEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.gitlab4j.api.webhook.EventCommit;
import org.gitlab4j.api.webhook.EventProject;
import org.gitlab4j.api.webhook.EventRepository;
import org.gitlab4j.api.webhook.PushEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

/**
 * @author Robin Müller
 */
public class PushHookTriggerHandlerImplTest {

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private PushHookTriggerHandler pushHookTriggerHandler;

    @Before
    public void setup() {
        pushHookTriggerHandler = new PushHookTriggerHandlerImpl(false);
    }

    @Test
    public void push_ciSkip() throws IOException, InterruptedException {
        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);
        PushEvent pushEvent = new PushEvent();
        EventCommit commit1 = new EventCommit();
        EventCommit commit2 = new EventCommit();
        commit1.setMessage("some message");
        commit2.setMessage("[ci-skip]");
        pushEvent.setCommits(Arrays.asList(commit1, commit2));
        pushHookTriggerHandler.handle(
                project,
                pushEvent,
                true,
                newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void push_build() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        Git.init().setDirectory(tmp.getRoot()).call();
        tmp.newFile("test");
        Git git = Git.open(tmp.getRoot());
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setMessage("test").call();
        ObjectId head = git.getRepository().resolve(Constants.HEAD);
        String repositoryUrl = tmp.getRoot().toURI().toString();

        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setScm(new GitSCM(repositoryUrl));
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);
        PushEvent pushEvent = new PushEvent();
        pushEvent.setUserId(123L);
        pushEvent.setUserName("admin@example");
        pushEvent.setProjectId(345L);
        EventRepository repository = new EventRepository();
        repository.setName("test-repo");
        repository.setUrl("git@gitlabserver.example.com:test-group/test-repo.git");
        repository.setHomepage("http://gitlabserver.example.com/test-group/test-repo");
        repository.setUrl("git@gitlab.org:test.git");
        repository.setGit_ssh_url("git@gitlab.org:test.git");
        repository.setGit_http_url("https://gitlab.org/test.git");
        pushEvent.setRepository(repository);
        pushEvent.setRef("refs/heads/" + git.nameRev().add(head).call().get(head));
        pushEvent.setBefore("0000000000000000000000000000000000000000");
        pushEvent.setAfter(commit.name());
        pushEvent.setProjectId(1L);
        pushEvent.setUserName("test");
        pushEvent.setObjectKind("tag_push");
        pushHookTriggerHandler.handle(
                project,
                pushEvent,
                true,
                newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void push_build2DifferentBranchesButSameCommit()
            throws IOException, InterruptedException, GitAPIException, ExecutionException {
        Git.init().setDirectory(tmp.getRoot()).call();
        tmp.newFile("test");
        Git git = Git.open(tmp.getRoot());
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setMessage("test").call();
        ObjectId head = git.getRepository().resolve(Constants.HEAD);
        String repositoryUrl = tmp.getRoot().toURI().toString();

        final AtomicInteger buildCount = new AtomicInteger(0);

        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setScm(new GitSCM(repositoryUrl));
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                int count = buildCount.incrementAndGet();
                if (count == 2) {
                    buildTriggered.signal();
                }
                return true;
            }
        });
        project.setQuietPeriod(0);
        PushEvent pushEvent = new PushEvent();
        pushEvent.setBefore("0000000000000000000000000000000000000000");
        pushEvent.setProjectId(1L);
        pushEvent.setUserName("test");
        pushEvent.setObjectKind("push");
        EventRepository repository = new EventRepository();
        repository.setName("test");
        repository.setHomepage("https://gitlab.org/test");
        repository.setUrl("git@gitlab.org:test.git");
        repository.setGit_ssh_url("git@gitlab.org:test.git");
        repository.setGit_http_url("https://gitlab.org/test.git");
        pushEvent.setRepository(repository);
        EventProject project1 = new EventProject();
        project1.setNamespace("test-namespace");
        project1.setWebUrl("https://gitlab.org/test");
        pushEvent.setProject(project1);
        pushEvent.setAfter(commit.name());
        pushEvent.setRef("refs/heads/" + git.nameRev().add(head).call().get(head));
        pushHookTriggerHandler.handle(
                project,
                pushEvent,
                true,
                newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));
        PushEvent pushEvent2 = new PushEvent();
        pushEvent2.setRef("refs/heads/" + git.nameRev().add(head).call().get(head) + "-2");
        pushHookTriggerHandler.handle(
                project,
                pushEvent2,
                true,
                newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));
        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
        assertThat(buildCount.intValue(), is(2));
    }

    @After
    public void after() {
        /*
         * Add Thread.sleep(5000) to avoid the following error on Windows:
         *
         *     Unable to delete 'C:\Jenkins\workspace\Plugins_gitlab-plugin_PR-1121\target\tmp\j h4861043637706712359'.
         *     Tried 3 times (of a maximum of 3) waiting 0.1 sec between attempts.
         */
        if (Functions.isWindows()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
}
