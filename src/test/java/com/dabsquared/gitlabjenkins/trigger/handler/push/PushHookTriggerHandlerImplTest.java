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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.PushHookBuilder;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.GitSCM;
import hudson.util.OneShotEvent;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author Robin Müller
 */
@WithJenkins
class PushHookTriggerHandlerImplTest {

    private static JenkinsRule jenkins;

    @TempDir
    private File tmp;

    private PushHookTriggerHandler pushHookTriggerHandler;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @BeforeEach
    void setUp() {
        pushHookTriggerHandler = new PushHookTriggerHandlerImpl(false);
    }

    @Test
    void push_ciSkip() throws Exception {
        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                buildHolder.set((FreeStyleBuild) build);
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);
        pushHookTriggerHandler.handle(
                project,
                pushHook()
                        .withCommits(Arrays.asList(
                                commit().withMessage("some message").build(),
                                commit().withMessage("[ci-skip]").build()))
                        .build(),
                true,
                newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    @Test
    void push_build() throws Exception {
        Git.init().setDirectory(tmp).call();
        File.createTempFile("test", null, tmp);
        Git git = Git.open(tmp);
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setSign(false).setMessage("test").call();
        ObjectId head = git.getRepository().resolve(Constants.HEAD);
        String repositoryUrl = tmp.toURI().toString();

        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setScm(new GitSCM(repositoryUrl));
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                buildHolder.set((FreeStyleBuild) build);
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);
        pushHookTriggerHandler.handle(
                project,
                pushHook()
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
                        .build(),
                true,
                newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void push_build2DifferentBranchesButSameCommit() throws Exception {
        Git.init().setDirectory(tmp).call();
        File.createTempFile("test", null, tmp);
        Git git = Git.open(tmp);
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setSign(false).setMessage("test").call();
        ObjectId head = git.getRepository().resolve(Constants.HEAD);
        String repositoryUrl = tmp.toURI().toString();

        final AtomicInteger buildCount = new AtomicInteger(0);

        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setConcurrentBuild(false);
        project.setScm(new GitSCM(repositoryUrl));
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                buildHolder.set((FreeStyleBuild) build);
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
        pushHookTriggerHandler.handle(
                project,
                pushHookBuilder.build(),
                true,
                newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));
        pushHookTriggerHandler.handle(
                project,
                pushHookBuilder
                        .but()
                        .withRef("refs/heads/" + git.nameRev().add(head).call().get(head) + "-2")
                        .build(),
                true,
                newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));
        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
        assertThat(buildCount.intValue(), is(2));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }
}
