package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestHookBuilder.mergeRequestHook;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestObjectAttributesBuilder.mergeRequestObjectAttributes;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.ProjectBuilder.project;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.UserBuilder.user;
import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilterFactory.newMergeRequestLabelFilter;
import static com.dabsquared.gitlabjenkins.trigger.handler.merge.MergeRequestHookTriggerHandlerFactory.withConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.Action;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestObjectAttributesBuilder;
import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterFactory;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.GitSCM;
import hudson.util.OneShotEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.runner.Description;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robin Müller
 */
public class MergeRequestHookTriggerHandlerImplTest {
    private static final Logger logger = LoggerFactory.getLogger(MergeRequestHookTriggerHandlerImplTest.class);

    @ClassRule
    public static JenkinsRule jenkins;

    static {
        // Every negative (or failing positive) test adds 10 seconds to run time. The default 180 seconds might not
        // suffice
        System.setProperty("jenkins.test.timeout", "450");
        jenkins = new JenkinsRule();
    }

    @Rule
    public TestName name = new TestName() {
        @Override
        protected void starting(Description d) {
            super.starting(d);
            logger.info(">> Starting test {}", getMethodName());
        }
    };

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void mergeRequest_ciSkip() throws Exception {
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        assertThat(ciSkipTestHelper("enable build", "enable build", buildHolder), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
        assertThat(ciSkipTestHelper("garbage [ci-skip] garbage", "enable build", buildHolder), is(false));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
        assertThat(ciSkipTestHelper("enable build", "garbage [ci-skip] garbage", buildHolder), is(false));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_opened_with_source() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.opened, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_opened_with_both() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.opened, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_opened_with_never() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.never)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, State.opened, Action.update, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    @Test
    public void mergeRequest_build_when_reopened() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.reopened, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_opened_with_approved_action_enabled() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnApprovedMergeRequest(true)
                .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.opened, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_accepted() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnAcceptedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.merged, Action.merge, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_accepted_with_approved_action_enabled() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnAcceptedMergeRequest(true)
                .setTriggerOnApprovedMergeRequest(true)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.merged, Action.merge, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_closed() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnClosedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.closed, Action.close, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_close() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnClosedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, Action.close, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_closed_with_actions_enabled() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnClosedMergeRequest(true)
                .setTriggerOnApprovedMergeRequest(true)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.closed, Action.close, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_do_not_build_for_accepted_when_nothing_enabled() throws Exception {
        do_not_build_for_state_when_nothing_enabled(State.merged);
    }

    @Test
    public void mergeRequest_do_not_build_for_updated_when_nothing_enabled() throws Exception {
        do_not_build_for_state_when_nothing_enabled(State.updated);
    }

    @Test
    public void mergeRequest_do_not_build_for_reopened_when_nothing_enabled() throws Exception {
        do_not_build_for_state_when_nothing_enabled(State.reopened);
    }

    @Test
    public void mergeRequest_do_not_build_for_opened_when_nothing_enabled() throws Exception {
        do_not_build_for_state_when_nothing_enabled(State.opened);
    }

    @Test
    public void mergeRequest_do_not_build_when_accepted_some_enabled() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
                .setTriggerOnApprovedMergeRequest(true)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.merged, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    @Test
    public void mergeRequest_build_for_accepted_state_when_approved_action_triggered() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnApprovedMergeRequest(true)
                .setTriggerOnAcceptedMergeRequest(true)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, State.merged, Action.approved, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_do_not_build_when_closed() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
                .setTriggerOnApprovedMergeRequest(true)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.closed, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    @Test
    public void mergeRequest_do_not_build_for_updated_state_and_approved_action_when_both_not_enabled()
            throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, State.updated, Action.approved, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    @Test
    public void mergeRequest_do_not_build_for_updated_state_and_approved_action_when_updated_enabled_but_approved_not()
            throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, State.updated, Action.approved, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    @Test
    public void mergeRequest_build_for_update_state_when_updated_state_and_approved_action_enabled() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnApprovedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, State.updated, Action.approved, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_for_update_state_and_action_when_updated_state_and_approved_action_enabled()
            throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnApprovedMergeRequest(true)
                .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, State.updated, Action.update, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_do_not_build_for_update_state_and_action_when_opened_state_and_approved_action_enabled()
            throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnApprovedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, State.updated, Action.update, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    @Test
    public void mergeRequest_build_for_update_state_when_updated_state_and_merge_action() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnAcceptedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, State.updated, Action.merge, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_for_approved_action_when_opened_state_and_approved_action_enabled()
            throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnApprovedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, State.updated, Action.approved, buildHolder);
        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_for_approved_action_when_only_approved_enabled() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnMergeRequest(false)
                .setTriggerOnApprovedMergeRequest(true)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, State.updated, Action.approved, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_new_commits_were_pushed_state_opened_action_open() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnMergeRequest(true)
                .setTriggerOnlyIfNewCommitsPushed(true)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.opened, Action.open, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_new_commits_were_pushed_state_reopened_action_reopen() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnMergeRequest(true)
                .setTriggerOnlyIfNewCommitsPushed(true)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, State.reopened, Action.reopen, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_new_commits_were_pushed_do_not_build_without_commits() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnMergeRequest(true)
                .setTriggerOnlyIfNewCommitsPushed(true)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, State.updated, Action.update, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    @Test
    public void mergeRequest_build_only_when_approved_and_not_when_updated() throws Exception {
        mergeRequest_build_only_when_approved(Action.update);
    }

    @Test
    public void mergeRequest_build_only_when_approved_and_not_when_opened() throws Exception {
        mergeRequest_build_only_when_approved(Action.open);
    }

    @Test
    public void mergeRequest_build_only_when_approved_and_not_when_merge() throws Exception {
        mergeRequest_build_only_when_approved(Action.merge);
    }

    @Test
    public void mergeRequest_build_only_when_state_modified() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnAcceptedMergeRequest(true)
                .setTriggerOnClosedMergeRequest(true)
                .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
                .build();
        Git.init().setDirectory(tmp.getRoot()).call();
        tmp.newFile("test");
        Git git = Git.open(tmp.getRoot());
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setSign(false).setMessage("test").call();
        ObjectId head = git.getRepository().resolve(Constants.HEAD);
        String repositoryUrl = tmp.getRoot().toURI().toString();

        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setScm(new GitSCM(repositoryUrl));
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                buildHolder.set((FreeStyleBuild) build);
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);
        MergeRequestObjectAttributesBuilder objectAttributes =
                defaultMergeRequestObjectAttributes().withAction(Action.update);
        mergeRequestHookTriggerHandler.handle(
                project,
                mergeRequestHook()
                        .withObjectAttributes(objectAttributes
                                .withTargetBranch("refs/heads/"
                                        + git.nameRev().add(head).call().get(head))
                                .withLastCommit(commit().withAuthor(
                                                user().withName("test").build())
                                        .withId(commit.getName())
                                        .build())
                                .build())
                        .withProject(project()
                                .withWebUrl("https://gitlab.org/test.git")
                                .build())
                        .withUser(user().withId(1)
                                .withName("User")
                                .withUsername("user")
                                .withEmail("user@gitlab.com")
                                .withAvatarUrl(
                                        "https://www.gravatar.com/avatar/e64c7d89f26bd1972efa854d13d7dd61?s=80&d=identicon")
                                .build())
                        .build(),
                true,
                BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
        MergeRequestObjectAttributesBuilder objectAttributes2 =
                defaultMergeRequestObjectAttributes().withState(State.merged).withAction(Action.merge);
        mergeRequestHookTriggerHandler.handle(
                project,
                mergeRequestHook()
                        .withObjectAttributes(objectAttributes2
                                .withTargetBranch("refs/heads/"
                                        + git.nameRev().add(head).call().get(head))
                                .withLastCommit(commit().withAuthor(
                                                user().withName("test").build())
                                        .withId(commit.getName())
                                        .build())
                                .build())
                        .withProject(project()
                                .withWebUrl("https://gitlab.org/test.git")
                                .build())
                        .withUser(user().withId(1)
                                .withName("User")
                                .withUsername("user")
                                .withEmail("user@gitlab.com")
                                .withAvatarUrl(
                                        "https://www.gravatar.com/avatar/e64c7d89f26bd1972efa854d13d7dd61?s=80&d=identicon")
                                .build())
                        .build(),
                true,
                BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    private void do_not_build_for_state_when_nothing_enabled(State state) throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnMergeRequest(false).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, state, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    private void mergeRequest_build_only_when_approved(Action action) throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnMergeRequest(false)
                .setTriggerOnApprovedMergeRequest(true)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, action, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    private OneShotEvent doHandle(
            MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler,
            Action action,
            AtomicReference<FreeStyleBuild> buildHolder)
            throws GitAPIException, IOException, InterruptedException {
        return doHandle(
                mergeRequestHookTriggerHandler,
                defaultMergeRequestObjectAttributes().withAction(action),
                buildHolder);
    }

    private OneShotEvent doHandle(
            MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler,
            State state,
            AtomicReference<FreeStyleBuild> buildHolder)
            throws GitAPIException, IOException, InterruptedException {
        return doHandle(
                mergeRequestHookTriggerHandler,
                defaultMergeRequestObjectAttributes().withState(state),
                buildHolder);
    }

    private OneShotEvent doHandle(
            MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler,
            State state,
            Action action,
            AtomicReference<FreeStyleBuild> buildHolder)
            throws GitAPIException, IOException, InterruptedException {
        return doHandle(
                mergeRequestHookTriggerHandler,
                defaultMergeRequestObjectAttributes().withState(state).withAction(action),
                buildHolder);
    }

    private OneShotEvent doHandle(
            MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler,
            MergeRequestObjectAttributesBuilder objectAttributes,
            AtomicReference<FreeStyleBuild> buildHolder)
            throws GitAPIException, IOException, InterruptedException {
        Git.init().setDirectory(tmp.getRoot()).call();
        tmp.newFile("test");
        Git git = Git.open(tmp.getRoot());
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setSign(false).setMessage("test").call();
        ObjectId head = git.getRepository().resolve(Constants.HEAD);
        String repositoryUrl = tmp.getRoot().toURI().toString();

        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setScm(new GitSCM(repositoryUrl));
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                buildHolder.set((FreeStyleBuild) build);
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);
        mergeRequestHookTriggerHandler.handle(
                project,
                mergeRequestHook()
                        .withObjectAttributes(objectAttributes
                                .withTargetBranch("refs/heads/"
                                        + git.nameRev().add(head).call().get(head))
                                .withLastCommit(commit().withAuthor(
                                                user().withName("test").build())
                                        .withId(commit.getName())
                                        .build())
                                .build())
                        .withProject(project()
                                .withWebUrl("https://gitlab.org/test.git")
                                .build())
                        .withUser(user().withId(1)
                                .withName("User")
                                .withUsername("user")
                                .withEmail("user@gitlab.com")
                                .withAvatarUrl(
                                        "https://www.gravatar.com/avatar/e64c7d89f26bd1972efa854d13d7dd61?s=80&d=identicon")
                                .build())
                        .build(),
                true,
                BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        return buildTriggered;
    }

    private boolean ciSkipTestHelper(
            String MRDescription, String lastCommitMsg, AtomicReference<FreeStyleBuild> buildHolder)
            throws IOException, InterruptedException {
        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                buildHolder.set((FreeStyleBuild) build);
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(
                Arrays.asList(State.opened, State.reopened), List.of(Action.approved), false, false, false);
        mergeRequestHookTriggerHandler.handle(
                project,
                mergeRequestHook()
                        .withObjectAttributes(defaultMergeRequestObjectAttributes()
                                .withDescription(MRDescription)
                                .withLastCommit(commit().withMessage(lastCommitMsg)
                                        .withAuthor(user().withName("test").build())
                                        .withId("testid")
                                        .build())
                                .build())
                        .withUser(user().withId(1)
                                .withName("User")
                                .withUsername("user")
                                .withEmail("user@gitlab.com")
                                .withAvatarUrl(
                                        "https://www.gravatar.com/avatar/e64c7d89f26bd1972efa854d13d7dd61?s=80&d=identicon")
                                .build())
                        .build(),
                true,
                BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        return buildTriggered.isSignaled();
    }

    private MergeRequestObjectAttributesBuilder defaultMergeRequestObjectAttributes() {
        return mergeRequestObjectAttributes()
                .withIid(1)
                .withAction(Action.open)
                .withState(State.opened)
                .withTitle("test")
                .withTargetProjectId(1)
                .withSourceProjectId(1)
                .withSourceBranch("feature")
                .withTargetBranch("master")
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
                        .build());
    }
}
