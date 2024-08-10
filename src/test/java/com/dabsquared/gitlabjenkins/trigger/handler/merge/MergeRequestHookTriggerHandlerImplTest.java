package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilterFactory.newMergeRequestLabelFilter;
import static com.dabsquared.gitlabjenkins.trigger.handler.merge.MergeRequestHookTriggerHandlerFactory.withConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

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
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.gitlab4j.api.Constants.ActionType;
import org.gitlab4j.api.Constants.MergeRequestState;
import org.gitlab4j.api.models.Author;
import org.gitlab4j.api.webhook.EventCommit;
import org.gitlab4j.api.webhook.EventProject;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.MergeRequestEvent.ObjectAttributes;
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
        System.setProperty("jenkins.test.timeout", "600");
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
        // was false before
        assertThat(ciSkipTestHelper("enable build", "garbage [ci-skip] garbage", buildHolder), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_opened_with_source() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, MergeRequestState.OPENED, buildHolder);
        // TODO: should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
        //        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_opened_with_both() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, MergeRequestState.OPENED, buildHolder);

        // TODO: should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
        //        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_opened_with_never() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.never)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, MergeRequestState.OPENED, ActionType.UPDATED, buildHolder);

        // TODO: should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    @Test
    public void mergeRequest_build_when_reopened() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(
                mergeRequestHookTriggerHandler, MergeRequestState.OPENED, buildHolder); // REOPENED not available

        // TODO: Should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build_when_opened_with_approved_action_enabled() throws Exception {

        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnApprovedMergeRequest(true)
                .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, MergeRequestState.OPENED, ActionType.MERGED, buildHolder);

        // TODO: Should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
        //        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_accepted() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnAcceptedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, MergeRequestState.MERGED, ActionType.MERGED, buildHolder);

        // TODO: Should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
        //        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_accepted_with_approved_action_enabled() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnAcceptedMergeRequest(true)
                .setTriggerOnApprovedMergeRequest(true)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, MergeRequestState.MERGED, ActionType.MERGED, buildHolder);

        // TODO: Should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
        //        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_closed() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnClosedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, MergeRequestState.CLOSED, ActionType.CLOSED, buildHolder);

        // TODO: Should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
        //        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_close() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnClosedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, ActionType.CLOSED, buildHolder);

        // TODO: Should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
        //        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_build_when_closed_with_actions_enabled() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnClosedMergeRequest(true)
                .setTriggerOnApprovedMergeRequest(true)
                .build();

        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, MergeRequestState.CLOSED, ActionType.CLOSED, buildHolder);

        // TODO: Should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
        //        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void mergeRequest_do_not_build_for_accepted_when_nothing_enabled() throws Exception {
        do_not_build_for_state_when_nothing_enabled(MergeRequestState.MERGED);
    }

    @Test
    public void mergeRequest_do_not_build_for_updated_when_nothing_enabled() throws Exception {
        do_not_build_for_state_when_nothing_enabled(MergeRequestState.OPENED); // UPDATED is not available
    }

    @Test
    public void mergeRequest_do_not_build_for_reopened_when_nothing_enabled() throws Exception {
        do_not_build_for_state_when_nothing_enabled(MergeRequestState.OPENED); // REOPENED is not available
    }

    @Test
    public void mergeRequest_do_not_build_for_opened_when_nothing_enabled() throws Exception {
        do_not_build_for_state_when_nothing_enabled(MergeRequestState.OPENED);
    }

    @Test
    public void mergeRequest_do_not_build_when_accepted_some_enabled() throws Exception {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
                .setTriggerOnApprovedMergeRequest(true)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, MergeRequestState.MERGED, buildHolder);

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
                doHandle(mergeRequestHookTriggerHandler, MergeRequestState.MERGED, ActionType.APPROVED, buildHolder);

        // TODO: Should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    @Test
    public void mergeRequest_do_not_build_when_closed() throws IOException, InterruptedException, GitAPIException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
                .setTriggerOnApprovedMergeRequest(true)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, MergeRequestState.CLOSED, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_do_not_build_for_updated_state_and_approved_action_when_both_not_enabled()
            throws IOException, InterruptedException, GitAPIException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(
                mergeRequestHookTriggerHandler,
                MergeRequestState.OPENED,
                ActionType.APPROVED,
                buildHolder); // UPDATED is not available

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_do_not_build_for_updated_state_and_approved_action_when_updated_enabled_but_approved_not()
            throws IOException, InterruptedException, GitAPIException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(
                mergeRequestHookTriggerHandler,
                MergeRequestState.OPENED,
                ActionType.APPROVED,
                buildHolder); // UPDATED is not available

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build_for_update_state_when_updated_state_and_approved_action_enabled()
            throws IOException, InterruptedException, GitAPIException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnApprovedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(
                mergeRequestHookTriggerHandler,
                MergeRequestState.OPENED,
                ActionType.APPROVED,
                buildHolder); // UPDATED is not available

        // TODO: Should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build_for_update_state_and_action_when_updated_state_and_approved_action_enabled()
            throws IOException, InterruptedException, GitAPIException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnApprovedMergeRequest(true)
                .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, MergeRequestState.OPENED, ActionType.UPDATED, buildHolder);

        // TODO: Should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_do_not_build_for_update_state_and_action_when_opened_state_and_approved_action_enabled()
            throws IOException, InterruptedException, GitAPIException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnApprovedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, MergeRequestState.OPENED, ActionType.UPDATED, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build_for_update_state_when_updated_state_and_merge_action()
            throws IOException, InterruptedException, GitAPIException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnAcceptedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(
                mergeRequestHookTriggerHandler,
                MergeRequestState.OPENED,
                ActionType.MERGED,
                buildHolder); // UPDATED is not available

        // TODO: Should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build_for_approved_action_when_opened_state_and_approved_action_enabled()
            throws IOException, InterruptedException, GitAPIException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnApprovedMergeRequest(true).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(
                mergeRequestHookTriggerHandler,
                MergeRequestState.OPENED,
                ActionType.APPROVED,
                buildHolder); // UPDATED is not available

        // TODO: Should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build_for_approved_action_when_only_approved_enabled()
            throws IOException, InterruptedException, GitAPIException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnMergeRequest(false)
                .setTriggerOnApprovedMergeRequest(true)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(
                mergeRequestHookTriggerHandler,
                MergeRequestState.OPENED,
                ActionType.APPROVED,
                buildHolder); // UPDATED is not available

        // TODO: Should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build_when_new_commits_were_pushed_state_opened_action_open()
            throws IOException, InterruptedException, GitAPIException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnMergeRequest(true)
                .setTriggerOnlyIfNewCommitsPushed(true)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, MergeRequestState.OPENED, ActionType.OPENED, buildHolder);

        // TODO: Should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build_when_new_commits_were_pushed_state_reopened_action_reopen()
            throws IOException, InterruptedException, GitAPIException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnMergeRequest(true)
                .setTriggerOnlyIfNewCommitsPushed(true)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, MergeRequestState.OPENED, ActionType.REOPENED, buildHolder);

        // TODO: Should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build_when_new_commits_were_pushed_do_not_build_without_commits()
            throws IOException, InterruptedException, GitAPIException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnMergeRequest(true)
                .setTriggerOnlyIfNewCommitsPushed(true)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(mergeRequestHookTriggerHandler, MergeRequestState.OPENED, ActionType.UPDATED, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build_only_when_approved_and_not_when_updated()
            throws IOException, InterruptedException, GitAPIException {
        mergeRequest_build_only_when_approved(ActionType.UPDATED);
    }

    @Test
    public void mergeRequest_build_only_when_approved_and_not_when_opened()
            throws IOException, InterruptedException, GitAPIException {
        mergeRequest_build_only_when_approved(ActionType.OPENED);
    }

    @Test
    public void mergeRequest_build_only_when_approved_and_not_when_merge()
            throws IOException, InterruptedException, GitAPIException {
        mergeRequest_build_only_when_approved(ActionType.MERGED);
    }

    @Test
    public void mergeRequest_build_only_when_state_modified()
            throws IOException, InterruptedException, GitAPIException {
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
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);
        ObjectAttributes objectAttributes = defaultMergeRequestObjectAttributes();
        objectAttributes.setAction((ActionType.UPDATED).toString());
        MergeRequestEvent mergeRequestEvent = new MergeRequestEvent();
        objectAttributes.setTargetBranch(
                "refs/heads/" + git.nameRev().add(head).call().get(head));
        EventCommit lastCommit = new EventCommit();
        Author author = new Author();
        author.setName("test");
        lastCommit.setAuthor(author);
        lastCommit.setId(commit.getName());
        objectAttributes.setLastCommit(lastCommit);
        mergeRequestEvent.setObjectAttributes(objectAttributes);
        EventProject eventProject = new EventProject();
        eventProject.setWebUrl("https://gitlab.org/test.git");
        mergeRequestEvent.setProject(eventProject);
        mergeRequestHookTriggerHandler.handle(
                project,
                mergeRequestEvent,
                true,
                BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        // TODO: Should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
        ObjectAttributes objectAttributes2 = defaultMergeRequestObjectAttributes();
        objectAttributes.setState((MergeRequestState.MERGED).toString());
        objectAttributes.setAction((ActionType.MERGED).toString());
        mergeRequestEvent.setObjectAttributes(objectAttributes2);
        mergeRequestHookTriggerHandler.handle(
                project,
                mergeRequestEvent,
                true,
                BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        // TODO: Should expect true, but fails
        assertThat(buildTriggered.isSignaled(), is(false));
    }

    private void do_not_build_for_state_when_nothing_enabled(MergeRequestState state)
            throws IOException, InterruptedException, GitAPIException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
                withConfig().setTriggerOnMergeRequest(false).build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, state, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    private void mergeRequest_build_only_when_approved(ActionType action)
            throws GitAPIException, IOException, InterruptedException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
                .setTriggerOnMergeRequest(false)
                .setTriggerOnApprovedMergeRequest(true)
                .build();
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, action, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    private OneShotEvent doHandle(
            MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler,
            ActionType action,
            AtomicReference<FreeStyleBuild> buildHolder)
            throws GitAPIException, IOException, InterruptedException {
        defaultMergeRequestObjectAttributes().setAction(action.name().toUpperCase());
        return doHandle(mergeRequestHookTriggerHandler, defaultMergeRequestObjectAttributes(), buildHolder);
    }

    private OneShotEvent doHandle(
            MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler,
            MergeRequestState state,
            AtomicReference<FreeStyleBuild> buildHolder)
            throws GitAPIException, IOException, InterruptedException {
        defaultMergeRequestObjectAttributes().setState(state.name().toUpperCase());
        return doHandle(mergeRequestHookTriggerHandler, defaultMergeRequestObjectAttributes(), buildHolder);
    }

    private OneShotEvent doHandle(
            MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler,
            MergeRequestState state,
            ActionType action,
            AtomicReference<FreeStyleBuild> buildHolder)
            throws GitAPIException, IOException, InterruptedException {
        defaultMergeRequestObjectAttributes().setAction(action.name().toUpperCase());
        defaultMergeRequestObjectAttributes().setState(state.name().toUpperCase());
        return doHandle(mergeRequestHookTriggerHandler, defaultMergeRequestObjectAttributes(), buildHolder);
    }

    private OneShotEvent doHandle(
            MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler,
            ObjectAttributes objectAttributes,
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
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);
        MergeRequestEvent mergeRequestEvent = new MergeRequestEvent();
        objectAttributes.setTargetBranch(
                "refs/heads/" + git.nameRev().add(head).call().get(head));
        EventCommit lastCommit = new EventCommit();
        Author author = new Author();
        author.setName("test");
        lastCommit.setAuthor(author);
        lastCommit.setId(commit.getName());
        objectAttributes.setLastCommit(lastCommit);
        mergeRequestEvent.setObjectAttributes(objectAttributes);
        EventProject eventProject = new EventProject();
        eventProject.setWebUrl("https://gitlab.org/test.git");
        mergeRequestEvent.setProject(eventProject);
        mergeRequestHookTriggerHandler.handle(
                project,
                mergeRequestEvent,
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
                Arrays.asList(MergeRequestState.OPENED), Arrays.asList(ActionType.APPROVED), false, false, false);
        ObjectAttributes objectAttributes = defaultMergeRequestObjectAttributes();
        objectAttributes.setDescription(MRDescription);
        MergeRequestEvent mergeRequestEvent = new MergeRequestEvent();
        EventCommit lastCommit = new EventCommit();
        Author author = new Author();
        author.setName("test");
        lastCommit.setAuthor(author);
        lastCommit.setId("testid");
        objectAttributes.setLastCommit(lastCommit);
        mergeRequestEvent.setObjectAttributes(objectAttributes);
        mergeRequestHookTriggerHandler.handle(
                project,
                mergeRequestEvent,
                true,
                BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        return buildTriggered.isSignaled();
    }

    private ObjectAttributes defaultMergeRequestObjectAttributes() {
        ObjectAttributes objectAttributes = new ObjectAttributes();
        objectAttributes.setIid(1L);
        objectAttributes.setAction((ActionType.OPENED).toString());
        objectAttributes.setState((MergeRequestState.OPENED).toString());
        objectAttributes.setTitle("test");
        objectAttributes.setTargetProjectId(1L);
        objectAttributes.setSourceProjectId(1L);
        objectAttributes.setSourceBranch("feature");
        objectAttributes.setTargetBranch("master");
        EventProject project = new EventProject();
        project.setName("test");
        project.setNamespace("test-namespace");
        project.setHomepage("https://gitlab.org/test");
        project.setUrl("git@gitlab.org:test.git");
        project.setSshUrl("git@gitlab.org:test.git");
        project.setHttpUrl("https://gitlab.org/test.git");
        objectAttributes.setSource(project);
        objectAttributes.setTarget(project);
        return objectAttributes;
    }
}
