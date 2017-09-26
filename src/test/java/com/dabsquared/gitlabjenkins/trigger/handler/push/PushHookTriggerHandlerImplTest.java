package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.Commit;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
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
import org.eclipse.jgit.errors.MissingObjectException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.ProjectBuilder.project;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.PushHookBuilder.pushHook;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.RepositoryBuilder.repository;
import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static com.dabsquared.gitlabjenkins.trigger.filter.FilterFactory.newBranchFilter;
import static com.dabsquared.gitlabjenkins.trigger.filter.FilterFactory.newFilesFilter;
import static com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilterFactory.newMergeRequestLabelFilter;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

/**
 * @author Robin Müller
 * @author Roland Hauser
 */
public class PushHookTriggerHandlerImplTest {

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final OneShotEvent buildTriggered = new OneShotEvent();
    private TestBuilder testBuilder = new TestBuilder() {
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            buildTriggered.signal();
            return true;
        }
    };

    private PushHookTriggerHandler pushHookTriggerHandler;
    private Git git;
    private String repositoryUrl;
    private RevCommit commit;
    private ObjectId head;

    private FreeStyleProject project;

    @Before
    public void setup() throws GitAPIException, IOException {
        pushHookTriggerHandler = new PushHookTriggerHandlerImpl();

        // Setup GIT-repo
        repositoryUrl = tmp.getRoot().toURI().toString();
        Git.init().setDirectory(tmp.getRoot()).call();
        tmp.newFile("test");
        git = Git.open(tmp.getRoot());
        git.add().addFilepattern("test");
        commit = git.commit().setMessage("test").call();
        head = git.getRepository().resolve(Constants.HEAD);

        // Setup project
        project = jenkins.createFreeStyleProject();
        project.setScm(new GitSCM(repositoryUrl));
        project.getBuildersList().add(testBuilder);
        project.setQuietPeriod(0);
    }

    @Test
    public void getCommits() {
        final List<Commit> commits = new ArrayList<>();
        PushHook hook = new PushHook();
        hook.setCommits(commits);
        PushHookTriggerHandlerImpl handler = new PushHookTriggerHandlerImpl();
        assertSame(commits, handler.getCommits(hook));
    }

    @Test
    public void push_ciSkip() throws IOException, InterruptedException {
        pushHookTriggerHandler.handle(project, pushHook()
                .withCommits(asList(commit().withMessage("some message").build(),
                                           commit().withMessage("[ci-skip]").build()))
                .build(), true, newFilesFilter(""), newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                                      newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(false));
    }

    private PushHookBuilder createPushHookBuilder() throws MissingObjectException, GitAPIException {
        return pushHook()
            .withBefore("0000000000000000000000000000000000000000")
            .withProjectId(1)
            .withUserName("test")
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
    }

    @Test
    public void push_build() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        pushHookTriggerHandler.handle(project, createPushHookBuilder()
                .build(), true, newFilesFilter(""),
                    newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                    newMergeRequestLabelFilter(null));
        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void push_build_noMatchingFilesInCommit() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        Commit notMatching = new Commit();
        notMatching.setAdded(asList("notMatching/resource"));
        pushHookTriggerHandler.handle(project, createPushHookBuilder()
                .withCommits(asList(notMatching))
                .build(), true,
                newFilesFilter("foo/bar/*"),
                newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
            newMergeRequestLabelFilter(null));

        buildTriggered.block(5000);
        assertThat(buildTriggered.isSignaled(), is(false));

        notMatching = new Commit();
        notMatching.setModified(asList("notMatching/resource"));
        pushHookTriggerHandler.handle(project, createPushHookBuilder()
                .withCommits(asList(notMatching))
                .build(), true,
            newFilesFilter("foo/bar/*"),
            newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
            newMergeRequestLabelFilter(null));

        buildTriggered.block(5000);
        assertThat(buildTriggered.isSignaled(), is(false));

        notMatching = new Commit();
        notMatching.setRemoved(asList("notMatching/resource"));
        pushHookTriggerHandler.handle(project, createPushHookBuilder()
                .withCommits(asList(notMatching))
                .build(), true,
            newFilesFilter("foo/bar/*"),
            newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
            newMergeRequestLabelFilter(null));

        buildTriggered.block(5000);
        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void push_build_matchingFilesInCommit() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        Commit matching = new Commit();
        matching.setAdded(asList("foo/bar/resource"));
        pushHookTriggerHandler.handle(project, createPushHookBuilder()
                .withCommits(asList(matching))
                .build(), true,
            newFilesFilter("foo/bar/.*"),
            newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
            newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));

        matching = new Commit();
        matching.setModified(asList("foo/bar/resource"));
        pushHookTriggerHandler.handle(project, createPushHookBuilder()
                .withCommits(asList(matching))
                .build(), true,
            newFilesFilter("foo/bar/.*"),
            newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
            newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));

        matching = new Commit();
        matching.setRemoved(asList("foo/bar/resource"));
        pushHookTriggerHandler.handle(project, createPushHookBuilder()
                .withCommits(asList(matching))
                .build(), true,
            newFilesFilter("foo/bar/.*"),
            newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
            newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void push_build_filesRegexIsNull() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        Commit matching = new Commit();
        matching.setAdded(asList("foo/bar/resource"));
        pushHookTriggerHandler.handle(project, createPushHookBuilder()
                .withCommits(asList(matching))
                .build(), true,
            newFilesFilter("foo/bar/.*"),
            newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
            newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));

        matching = new Commit();
        matching.setModified(asList("foo/bar/resource"));
        pushHookTriggerHandler.handle(project, createPushHookBuilder()
                .withCommits(asList(matching))
                .build(), true,
            newFilesFilter("foo/bar/.*"),
            newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
            newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));

        matching = new Commit();
        matching.setRemoved(asList("foo/bar/resource"));
        pushHookTriggerHandler.handle(project, createPushHookBuilder()
                .withCommits(asList(matching))
                .build(), true,
            newFilesFilter(null),
            newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
            newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void push_build2DifferentBranchesButSameCommit() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        final AtomicInteger buildCount = new AtomicInteger(0);
        project.getBuildersList().remove(testBuilder);
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
                                      newFilesFilter(""),
                                      newMergeRequestLabelFilter(null));
        pushHookTriggerHandler.handle(project, pushHookBuilder
                                          .but().withRef("refs/heads/" + git.nameRev().add(head).call().get(head) + "-2").build(), true,
                                      newFilesFilter(""),
                                      newBranchFilter(branchFilterConfig().build(BranchFilterType.All)), newMergeRequestLabelFilter(null));
        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
        assertThat(buildCount.intValue(), is(2));
    }
}
