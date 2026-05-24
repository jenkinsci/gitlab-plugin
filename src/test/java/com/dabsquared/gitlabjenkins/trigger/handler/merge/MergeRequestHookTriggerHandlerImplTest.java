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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
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
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author Robin Müller
 */
@WithJenkins
class MergeRequestHookTriggerHandlerImplTest {

    private static JenkinsRule jenkins;

    @TempDir
    private File tmp;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkins = rule;
        jenkins.timeout = 450;
    }

    @Test
    void mergeRequest_ciSkip() throws Exception {
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        assertThat(ciSkipTestHelper("enable build", "enable build", buildHolder), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
        assertThat(ciSkipTestHelper("garbage [ci-skip] garbage", "enable build", buildHolder), is(false));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
        assertThat(ciSkipTestHelper("enable build", "garbage [ci-skip] garbage", buildHolder), is(false));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void mergeRequest_build_when_opened_with_source() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.opened, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void mergeRequest_build_when_opened_with_both() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.opened, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void mergeRequest_build_when_opened_with_never() throws Exception {
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
    void mergeRequest_build_when_reopened() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.reopened, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void mergeRequest_build_when_opened_with_approved_action_enabled() throws Exception {
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
    void mergeRequest_build_when_accepted() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnAcceptedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.merged, Action.merge, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void mergeRequest_build_when_accepted_with_approved_action_enabled() throws Exception {
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
    void mergeRequest_build_when_closed() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnClosedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.closed, Action.close, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void mergeRequest_build_when_close() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnClosedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, Action.close, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void mergeRequest_build_when_closed_with_actions_enabled() throws Exception {
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
    void mergeRequest_do_not_build_for_accepted_when_nothing_enabled() throws Exception {
        do_not_build_for_state_when_nothing_enabled(State.merged);
    }

    @Test
    void mergeRequest_do_not_build_for_updated_when_nothing_enabled() throws Exception {
        do_not_build_for_state_when_nothing_enabled(State.updated);
    }

    @Test
    void mergeRequest_do_not_build_for_reopened_when_nothing_enabled() throws Exception {
        do_not_build_for_state_when_nothing_enabled(State.reopened);
    }

    @Test
    void mergeRequest_do_not_build_for_opened_when_nothing_enabled() throws Exception {
        do_not_build_for_state_when_nothing_enabled(State.opened);
    }

    @Test
    void mergeRequest_do_not_build_when_accepted_some_enabled() throws Exception {
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
    void mergeRequest_build_for_accepted_state_when_approved_action_triggered() throws Exception {
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
    void mergeRequest_do_not_build_when_closed() throws Exception {
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
    void mergeRequest_do_not_build_for_updated_state_and_approved_action_when_both_not_enabled() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, State.updated, Action.approved, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    @Test
    void mergeRequest_do_not_build_for_updated_state_and_approved_action_when_updated_enabled_but_approved_not()
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
    void mergeRequest_build_for_update_state_when_updated_state_and_approved_action_enabled() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnApprovedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, State.updated, Action.approved, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void mergeRequest_build_for_update_state_and_action_when_updated_state_and_approved_action_enabled()
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
    void mergeRequest_do_not_build_for_update_state_and_action_when_opened_state_and_approved_action_enabled()
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
    void mergeRequest_build_for_update_state_when_updated_state_and_merge_action() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnAcceptedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, State.updated, Action.merge, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void mergeRequest_build_for_approved_action_when_opened_state_and_approved_action_enabled() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnApprovedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, State.updated, Action.approved, buildHolder);
        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void mergeRequest_build_for_approved_action_when_only_approved_enabled() throws Exception {
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
    void mergeRequest_build_when_new_commits_were_pushed_state_opened_action_open() throws Exception {
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
    void mergeRequest_build_when_new_commits_were_pushed_state_reopened_action_reopen() throws Exception {
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
    void mergeRequest_build_when_new_commits_were_pushed_do_not_build_without_commits() throws Exception {
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
    void mergeRequest_build_only_when_approved_and_not_when_updated() throws Exception {
        mergeRequest_build_only_when_approved(Action.update);
    }

    @Test
    void mergeRequest_build_only_when_approved_and_not_when_opened() throws Exception {
        mergeRequest_build_only_when_approved(Action.open);
    }

    @Test
    void mergeRequest_build_only_when_approved_and_not_when_merge() throws Exception {
        mergeRequest_build_only_when_approved(Action.merge);
    }

    @Test
    void mergeRequest_build_only_when_state_modified() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnAcceptedMergeRequest(true)
                .setTriggerOnClosedMergeRequest(true)
                .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
                .build();
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
                        .build(),
                true,
                BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void
            mergeRequest_skips_build_when_not_push_and_not_merge_request_and_accepted_and_trigger_open_merge_request_unspecified()
                    throws Exception {
        GitLabPushTrigger gitLabPushTrigger = new GitLabPushTrigger();
        gitLabPushTrigger.setTriggerOnPush(false);
        gitLabPushTrigger.setTriggerOnMergeRequest(false);
        gitLabPushTrigger.setTriggerOnAcceptedMergeRequest(true);
        gitLabPushTrigger.setBranchFilterType(BranchFilterType.All);

        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                MergeRequestHookTriggerHandlerFactory.newMergeRequestHookTriggerHandler(gitLabPushTrigger);
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, State.opened, Action.update, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
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
            throws Exception {
        return doHandle(
                mergeRequestHookTriggerHandler,
                defaultMergeRequestObjectAttributes().withAction(action),
                buildHolder);
    }

    private OneShotEvent doHandle(
            MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler,
            State state,
            AtomicReference<FreeStyleBuild> buildHolder)
            throws Exception {
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
            throws Exception {
        return doHandle(
                mergeRequestHookTriggerHandler,
                defaultMergeRequestObjectAttributes().withState(state).withAction(action),
                buildHolder);
    }

    private OneShotEvent doHandle(
            MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler,
            MergeRequestObjectAttributesBuilder objectAttributes,
            AtomicReference<FreeStyleBuild> buildHolder)
            throws Exception {
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
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
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
                        .build(),
                true,
                BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        return buildTriggered;
    }

    private boolean ciSkipTestHelper(
            String MRDescription, String lastCommitMsg, AtomicReference<FreeStyleBuild> buildHolder) throws Exception {
        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                buildHolder.set((FreeStyleBuild) build);
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(
                Arrays.asList(State.opened, State.reopened), Arrays.asList(Action.approved), false, false, false);
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
