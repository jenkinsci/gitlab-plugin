package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterFactory;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
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

import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestHookBuilder.mergeRequestHook;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestObjectAttributesBuilder.mergeRequestObjectAttributes;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.ProjectBuilder.project;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.UserBuilder.user;
import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilterFactory.newMergeRequestLabelFilter;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Robin Müller
 */
public class MergeRequestHookTriggerHandlerImplTest {

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler;

    @Before
    public void setup() {
        mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened, State.reopened), false);
    }

    @Test
    public void mergeRequest_ciSkip() throws IOException, InterruptedException {
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
        mergeRequestHookTriggerHandler.handle(project, mergeRequestHook()
                .withObjectAttributes(mergeRequestObjectAttributes().withDescription("[ci-skip]").build())
                .build(), true, BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                                              newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build() throws IOException, InterruptedException, GitAPIException, ExecutionException {
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
        mergeRequestHookTriggerHandler.handle(project, mergeRequestHook()
                .withObjectAttributes(mergeRequestObjectAttributes()
                        .withTargetBranch("refs/heads/" + git.nameRev().add(head).call().get(head))
                        .withState(State.opened)
                        .withIid(1)
                        .withTitle("test")
                        .withTargetProjectId(1)
                        .withSourceProjectId(1)
                        .withSourceBranch("feature")
                        .withTargetBranch("master")
                        .withLastCommit(commit().withAuthor(user().withName("test").build()).withId(commit.getName()).build())
                        .withSource(project()
                                .withName("test")
                                .withNamespace("test-namespace")
                                .withHomepage("https://gitlab.org/test")
                                .withUrl("git@gitlab.org:test.git")
                                .withSshUrl("git@gitlab.org:test.git")
                                .withHttpUrl("https://gitlab.org/test.git")
                                .build())
                        .withTarget(project()
                                .withName("test")
                                .withNamespace("test-namespace")
                                .withHomepage("https://gitlab.org/test")
                                .withUrl("git@gitlab.org:test.git")
                                .withSshUrl("git@gitlab.org:test.git")
                                .withHttpUrl("https://gitlab.org/test.git")
                                .build())
                        .build())
                .withProject(project()
                    .withWebUrl("https://gitlab.org/test.git")
                    .build()
                )
                .build(), true, BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                                              newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequestUpdated_noGitSCMSet_alreadyBuilt_noBuildTriggered() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        Git.init().setDirectory(tmp.getRoot()).call();
        tmp.newFile("test");
        Git git = Git.open(tmp.getRoot());
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setMessage("test").call();
        ObjectId head = git.getRepository().resolve(Constants.HEAD);

        final FreeStyleProject project = jenkins.createFreeStyleProject();
        final BuildNotifier buildNotifier = setupProjectWithNoGitScm(project);
        assertThat(project.getLastBuild(), nullValue());

        mergeRequestHookTriggerHandler.handle(project, mergeRequestHook()
            .withObjectAttributes(mergeRequestObjectAttributes()
                .withTargetBranch("refs/heads/" + git.nameRev().add(head).call().get(head))
                .withState(State.opened)
                .withIid(1)
                .withTitle("test")
                .withTargetProjectId(1)
                .withSourceProjectId(1)
                .withSourceBranch("feature")
                .withTargetBranch("master")
                .withLastCommit(commit().withAuthor(user().withName("test").build()).withId(commit.getName()).build())
                .withSource(project()
                    .withName("test")
                    .withNamespace("test-namespace")
                    .withHomepage("https://gitlab.org/test")
                    .withUrl("git@gitlab.org:test.git")
                    .withSshUrl("git@gitlab.org:test.git")
                    .withHttpUrl("https://gitlab.org/test.git")
                    .build())
                .withTarget(project()
                    .withName("test")
                    .withNamespace("test-namespace")
                    .withHomepage("https://gitlab.org/test")
                    .withUrl("git@gitlab.org:test.git")
                    .withSshUrl("git@gitlab.org:test.git")
                    .withHttpUrl("https://gitlab.org/test.git")
                    .build())
                .build())
            .build(), false, BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)));

        final FreeStyleBuild lastBuild = assertFirstBuildTriggered(project, buildNotifier);

        mergeRequestHookTriggerHandler.handle(project, mergeRequestHook()
            .withObjectAttributes(mergeRequestObjectAttributes()
                .withTargetBranch("refs/heads/" + git.nameRev().add(head).call().get(head))
                .withState(State.opened)
                .withIid(1)
                .withTitle("BRAND NEW TITLE")
                .withTargetProjectId(1)
                .withSourceProjectId(1)
                .withSourceBranch("feature")
                .withTargetBranch("master")
                .withLastCommit(commit().withAuthor(user().withName("test").build()).withId(commit.getName()).build())
                .withSource(project()
                    .withName("test")
                    .withNamespace("test-namespace")
                    .withHomepage("https://gitlab.org/test")
                    .withUrl("git@gitlab.org:test.git")
                    .withSshUrl("git@gitlab.org:test.git")
                    .withHttpUrl("https://gitlab.org/test.git")
                    .build())
                .withTarget(project()
                    .withName("test")
                    .withNamespace("test-namespace")
                    .withHomepage("https://gitlab.org/test")
                    .withUrl("git@gitlab.org:test.git")
                    .withSshUrl("git@gitlab.org:test.git")
                    .withHttpUrl("https://gitlab.org/test.git")
                    .build())
                .build())
            .build(), false, BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)));

        buildNotifier.getLock().block(10000);
        assertThat(project.getLastBuild(), is(lastBuild));
    }

    @Test
    public void mergeRequestUpdated_noGitSCMSet_newCommit_buildTriggered() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        Git.init().setDirectory(tmp.getRoot()).call();
        tmp.newFile("test");
        Git git = Git.open(tmp.getRoot());
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setMessage("test").call();
        ObjectId head = git.getRepository().resolve(Constants.HEAD);

        final FreeStyleProject project = jenkins.createFreeStyleProject();
        assertThat(project.getLastBuild(), nullValue());

        final BuildNotifier buildNotifier = setupProjectWithNoGitScm(project);

        mergeRequestHookTriggerHandler.handle(project, mergeRequestHook()
            .withObjectAttributes(mergeRequestObjectAttributes()
                .withTargetBranch("refs/heads/" + git.nameRev().add(head).call().get(head))
                .withState(State.opened)
                .withIid(1)
                .withTitle("test")
                .withTargetProjectId(1)
                .withSourceProjectId(1)
                .withSourceBranch("feature")
                .withTargetBranch("master")
                .withLastCommit(commit().withAuthor(user().withName("test").build()).withId(commit.getName()).build())
                .withSource(project()
                    .withName("test")
                    .withNamespace("test-namespace")
                    .withHomepage("https://gitlab.org/test")
                    .withUrl("git@gitlab.org:test.git")
                    .withSshUrl("git@gitlab.org:test.git")
                    .withHttpUrl("https://gitlab.org/test.git")
                    .build())
                .withTarget(project()
                    .withName("test")
                    .withNamespace("test-namespace")
                    .withHomepage("https://gitlab.org/test")
                    .withUrl("git@gitlab.org:test.git")
                    .withSshUrl("git@gitlab.org:test.git")
                    .withHttpUrl("https://gitlab.org/test.git")
                    .build())
                .build())
            .build(), false, BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)));

        final FreeStyleBuild lastBuild = assertFirstBuildTriggered(project, buildNotifier);

        mergeRequestHookTriggerHandler.handle(project, mergeRequestHook()
            .withObjectAttributes(mergeRequestObjectAttributes()
                .withTargetBranch("refs/heads/" + git.nameRev().add(head).call().get(head))
                .withState(State.opened)
                .withIid(1)
                .withTitle("test")
                .withTargetProjectId(1)
                .withSourceProjectId(1)
                .withSourceBranch("feature")
                .withTargetBranch("master")
                .withLastCommit(commit().withAuthor(user().withName("test").build()).withId("NEW COMMIT ID").build())
                .withSource(project()
                    .withName("test")
                    .withNamespace("test-namespace")
                    .withHomepage("https://gitlab.org/test")
                    .withUrl("git@gitlab.org:test.git")
                    .withSshUrl("git@gitlab.org:test.git")
                    .withHttpUrl("https://gitlab.org/test.git")
                    .build())
                .withTarget(project()
                    .withName("test")
                    .withNamespace("test-namespace")
                    .withHomepage("https://gitlab.org/test")
                    .withUrl("git@gitlab.org:test.git")
                    .withSshUrl("git@gitlab.org:test.git")
                    .withHttpUrl("https://gitlab.org/test.git")
                    .build())
                .build())
            .build(), false, BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)));

        buildNotifier.getLock().block(10000);
        assertThat(project.getLastBuild(), not(is(lastBuild)));
    }

    private BuildNotifier setupProjectWithNoGitScm(final FreeStyleProject project) throws IOException {
        final BuildNotifier buildNotifier = new BuildNotifier(0, new LockWrapper());
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                buildNotifier.addBuild();
                buildNotifier.getLock().signal();
                return true;
            }
        });
        project.setQuietPeriod(0);
        return buildNotifier;
    }

    private class BuildNotifier {
        private int buildsTriggered;
        private final LockWrapper lock;

        public BuildNotifier(final int buildsTriggered, final LockWrapper lock) {
            this.buildsTriggered = buildsTriggered;
            this.lock = lock;
        }

        public int getBuildsTriggered() {
            return buildsTriggered;
        }

        public LockWrapper getLock() {
            return lock;
        }

        public void addBuild() {
            buildsTriggered++;
        }
    }
	
	 private FreeStyleBuild assertFirstBuildTriggered(final FreeStyleProject project, final BuildNotifier buildNotifier) throws InterruptedException {
        buildNotifier.getLock().block(10000);
        assertThat(buildNotifier.getBuildsTriggered(), is(1));
        final FreeStyleBuild lastBuild = project.getLastBuild();
        assertThat(lastBuild, notNullValue());
        return lastBuild;
    }

}
