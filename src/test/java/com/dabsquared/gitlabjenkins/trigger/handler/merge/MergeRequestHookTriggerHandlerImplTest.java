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

import com.dabsquared.gitlabjenkins.gitlab.hook.model.Action;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestObjectAttributesBuilder;
import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterFactory;
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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
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
    public void mergeRequest_ciSkip() throws IOException, InterruptedException {
        assertThat(ciSkipTestHelper("enable build","enable build"), is(true));
        assertThat(ciSkipTestHelper("garbage [ci-skip] garbage","enable build"), is(false));
        assertThat(ciSkipTestHelper("enable build","garbage [ci-skip] garbage"), is(false));
    }

    @Test
    public void mergeRequest_build_when_opened() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig().build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.opened);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_when_reopened() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.reopened);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_when_opened_with_approved_action_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOnApprovedMergeRequest(true)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.opened);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_when_accepted() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOnAcceptedMergeRequest(true)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.merged, Action.merge);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_when_accepted_with_approved_action_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOnAcceptedMergeRequest(true)
            .setTriggerOnApprovedMergeRequest(true)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.merged, Action.merge);

        assertThat(buildTriggered.isSignaled(), is(true));
    }


    @Test
    public void mergeRequest_build_when_closed() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOnClosedMergeRequest(true)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.closed, Action.closed);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_when_closed_with_actions_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOnClosedMergeRequest(true)
            .setTriggerOnApprovedMergeRequest(true)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.closed, Action.closed);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_do_not_build_for_accepted_when_nothing_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        do_not_build_for_state_when_nothing_enabled(State.merged);
    }

    @Test
    public void mergeRequest_do_not_build_for_updated_when_nothing_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        do_not_build_for_state_when_nothing_enabled(State.updated);
    }

    @Test
    public void mergeRequest_do_not_build_for_reopened_when_nothing_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        do_not_build_for_state_when_nothing_enabled(State.reopened);
    }

    @Test
    public void mergeRequest_do_not_build_for_opened_when_nothing_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        do_not_build_for_state_when_nothing_enabled(State.opened);
    }

    @Test
    public void mergeRequest_do_not_build_when_accepted_some_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
            .setTriggerOnApprovedMergeRequest(true)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.merged);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build_for_accepted_state_when_approved_action_triggered() throws IOException, InterruptedException, GitAPIException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOnApprovedMergeRequest(true)
            .setTriggerOnAcceptedMergeRequest(true)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.merged, Action.approved);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_do_not_build_when_closed() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
            .setTriggerOnApprovedMergeRequest(true)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.closed);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_do_not_build_for_updated_state_and_approved_action_when_both_not_enabled() throws IOException, InterruptedException, GitAPIException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.updated, Action.approved);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_do_not_build_for_updated_state_and_approved_action_when_updated_enabled_but_approved_not() throws IOException, InterruptedException, GitAPIException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler =
            withConfig()
                .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.updated, Action.approved);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build_for_update_state_when_updated_state_and_approved_action_enabled() throws IOException, InterruptedException, GitAPIException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOnApprovedMergeRequest(true)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.updated, Action.approved);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_for_update_state_and_action_when_updated_state_and_approved_action_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOnApprovedMergeRequest(true)
            .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.source)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.updated, Action.update);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_do_not_build_for_update_state_and_action_when_opened_state_and_approved_action_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOnApprovedMergeRequest(true)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.updated, Action.update);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build_for_update_state_when_updated_state_and_merge_action() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOnAcceptedMergeRequest(true)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.updated, Action.merge);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_for_approved_action_when_opened_state_and_approved_action_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOnApprovedMergeRequest(true)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.updated, Action.approved);
        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_for_approved_action_when_only_approved_enabled() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOnMergeRequest(false)
            .setTriggerOnApprovedMergeRequest(true)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.updated, Action.approved);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_when_new_commits_were_pushed_state_opened_action_open() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOnMergeRequest(true)
            .setTriggerOnlyIfNewCommitsPushed(true)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.opened, Action.open);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_when_new_commits_were_pushed_state_reopened_action_reopen() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOnMergeRequest(true)
            .setTriggerOnlyIfNewCommitsPushed(true)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.reopened, Action.reopen);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_when_new_commits_were_pushed_do_not_build_without_commits() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOnMergeRequest(true)
            .setTriggerOnlyIfNewCommitsPushed(true)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.updated, Action.update);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build_only_when_approved_and_not_when_updated() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        mergeRequest_build_only_when_approved(Action.update);
    }

    @Test
    public void mergeRequest_build_only_when_approved_and_not_when_opened() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        mergeRequest_build_only_when_approved(Action.open);
    }

    @Test
    public void mergeRequest_build_only_when_approved_and_not_when_merge() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        mergeRequest_build_only_when_approved(Action.merge);
    }

    @Test
    public void mergeRequest_build_only_when_state_modified()throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOnAcceptedMergeRequest(true)
            .setTriggerOnClosedMergeRequest(true)
            .setTriggerOpenMergeRequest(TriggerOpenMergeRequest.never)
            .build();
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
        MergeRequestObjectAttributesBuilder objectAttributes = defaultMergeRequestObjectAttributes().withAction(Action.update);
        mergeRequestHookTriggerHandler.handle(project, mergeRequestHook()
                .withObjectAttributes(objectAttributes
                    .withTargetBranch("refs/heads/" + git.nameRev().add(head).call().get(head))
                    .withLastCommit(commit().withAuthor(user().withName("test").build()).withId(commit.getName()).build())
                    .build())
                .withProject(project()
                    .withWebUrl("https://gitlab.org/test.git")
                    .build()
                )
                .build(), true, BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
            newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
        MergeRequestObjectAttributesBuilder objectAttributes2 = defaultMergeRequestObjectAttributes().withState(State.merged).withAction(Action.merge);
        mergeRequestHookTriggerHandler.handle(project, mergeRequestHook()
                .withObjectAttributes(objectAttributes2
                    .withTargetBranch("refs/heads/" + git.nameRev().add(head).call().get(head))
                    .withLastCommit(commit().withAuthor(user().withName("test").build()).withId(commit.getName()).build())
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

    private void do_not_build_for_state_when_nothing_enabled(State state) throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOnMergeRequest(false)
            .build();
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, state);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

	private void mergeRequest_build_only_when_approved(Action action)
			throws GitAPIException, IOException, InterruptedException {
		MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = withConfig()
            .setTriggerOnMergeRequest(false)
            .setTriggerOnApprovedMergeRequest(true)
            .build();
	    OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, action);

	    assertThat(buildTriggered.isSignaled(), is(false));
	}

    private OneShotEvent doHandle(MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler, Action action) throws GitAPIException, IOException, InterruptedException {
        return doHandle(mergeRequestHookTriggerHandler, defaultMergeRequestObjectAttributes().withAction(action));
    }

    private OneShotEvent doHandle(MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler, State state) throws GitAPIException, IOException, InterruptedException {
        return doHandle(mergeRequestHookTriggerHandler, defaultMergeRequestObjectAttributes().withState(state));
    }

    private OneShotEvent doHandle(MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler, State state, Action action) throws GitAPIException, IOException, InterruptedException {
        return doHandle(mergeRequestHookTriggerHandler, defaultMergeRequestObjectAttributes().withState(state).withAction(action));
    }

	private OneShotEvent doHandle(MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler,
			MergeRequestObjectAttributesBuilder objectAttributes) throws GitAPIException, IOException,
            InterruptedException {
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
                .withObjectAttributes(objectAttributes
            		    .withTargetBranch("refs/heads/" + git.nameRev().add(head).call().get(head))
            		    .withLastCommit(commit().withAuthor(user().withName("test").build()).withId(commit.getName()).build())
                    .build())
                .withProject(project()
                    .withWebUrl("https://gitlab.org/test.git")
                    .build()
                )
                .build(), true, BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
            newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        return buildTriggered;
	}

    private boolean ciSkipTestHelper(String MRDescription, String lastCommitMsg) throws IOException, InterruptedException {
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
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened, State.reopened), Arrays.asList(Action.approved), false, false, false);
        mergeRequestHookTriggerHandler.handle(project, mergeRequestHook()
                .withObjectAttributes(defaultMergeRequestObjectAttributes().withDescription(MRDescription).withLastCommit(commit().withMessage(lastCommitMsg).withAuthor(user().withName("test").build()).withId("testid").build()).build())
                .build(), true, BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
            newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        return buildTriggered.isSignaled();
    }

	private MergeRequestObjectAttributesBuilder defaultMergeRequestObjectAttributes() {
		return mergeRequestObjectAttributes()
		    .withIid(1)
            .withAction(Action.update)
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

	@After
    /* Add sleep(5000) on after to avoid following error on Windows test
        Unable to delete 'C:\Jenkins\workspace\Plugins_gitlab-plugin_PR-1121\target\tmp\j h4861043637706712359'. Tried 3 times (of a maximum of 3) waiting 0.1 sec between attempts.
     */
	public void  after()
    {
        try {
            if (Functions.isWindows()) {
                Thread.sleep(5000);
            }
        } catch (InterruptedException ignored) {

        }
    }
}
