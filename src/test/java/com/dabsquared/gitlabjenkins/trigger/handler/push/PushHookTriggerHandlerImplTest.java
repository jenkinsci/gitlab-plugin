package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.PushHookBuilder;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.GitSCM;
import hudson.util.OneShotEvent;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.ProjectBuilder.project;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.PushHookBuilder.pushHook;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.RepositoryBuilder.repository;
import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterFactory.newBranchFilter;
import static com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilterFactory.newMergeRequestLabelFilter;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
        pushHookTriggerHandler = new PushHookTriggerHandlerImpl();
    }

    @Test
    public void push_ciSkip() throws IOException, InterruptedException {
        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);
        pushHookTriggerHandler.handle(project, pushHook()
                .withCommits(Arrays.asList(commit().withMessage("some message").build(),
                                           commit().withMessage("[ci-skip]").build()))
                .build(), true, newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
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
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);
        pushHookTriggerHandler.handle(project, pushHook()
                .withBefore("0000000000000000000000000000000000000000")
                .withProjectId(1)
                .withUserName("test")
                .withObjectKind("tag_push")
                .withRepository(repository()
                        .withName("test")
                        .withHomepage("https://gitlab.org/test")
                        .withUrl("git@gitlab.org:test.git")
                        .withGitSshUrl("git@gitlab.org:test.git")
                        .withGitHttpUrl("https://gitlab.org/test.git")
                        .build())
                .withProject(project()
                        .withNamespace("test-namespace")
                        .withWebUrl("https://gitlab.org/test")
                        .build())
                .withAfter(commit.name())
                .withRef("refs/heads/" + git.nameRev().add(head).call().get(head))
                .build(), true, newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                                      newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void push_build2DifferentBranchesButSameCommit() throws IOException, InterruptedException, GitAPIException, ExecutionException {
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
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                int count = buildCount.incrementAndGet();
                if (count == 2) {
                    buildTriggered.signal();
                }
                return true;
            }
        });
        project.setQuietPeriod(0);
        PushHookBuilder pushHookBuilder = pushHook()
            .withBefore("0000000000000000000000000000000000000000")
            .withProjectId(1)
            .withUserName("test")
            .withObjectKind("push")
            .withRepository(repository()
                                .withName("test")
                                .withHomepage("https://gitlab.org/test")
                                .withUrl("git@gitlab.org:test.git")
                                .withGitSshUrl("git@gitlab.org:test.git")
                                .withGitHttpUrl("https://gitlab.org/test.git")
                                .build())
            .withProject(project()
                             .withNamespace("test-namespace")
                             .withWebUrl("https://gitlab.org/test")
                             .build())
            .withAfter(commit.name())
            .withRef("refs/heads/" + git.nameRev().add(head).call().get(head));
        pushHookTriggerHandler.handle(project, pushHookBuilder.build(), true, newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                                      newMergeRequestLabelFilter(null));
        pushHookTriggerHandler.handle(project, pushHookBuilder
                                          .but().withRef("refs/heads/" + git.nameRev().add(head).call().get(head) + "-2").build(), true,
                                      newBranchFilter(branchFilterConfig().build(BranchFilterType.All)), newMergeRequestLabelFilter(null));
        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
        assertThat(buildCount.intValue(), is(2));
    }
}
