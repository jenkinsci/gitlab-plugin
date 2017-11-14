package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterFactory;
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
import static org.junit.Assert.assertThat;

/**
 * @author Robin Müller
 */
public class MergeRequestHookTriggerHandlerImplTest {

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

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
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened, State.reopened), false);
        mergeRequestHookTriggerHandler.handle(project, mergeRequestHook()
                .withObjectAttributes(mergeRequestObjectAttributes().withDescription("[ci-skip]").build())
                .build(), true, BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                                              newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_build() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened, State.reopened), false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.opened);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_when_accepted() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.merged), false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.merged);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_build_when_closed() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.closed), false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.closed);

        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void mergeRequest_do_not_build_when_accepted() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened, State.updated), false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.merged);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void mergeRequest_do_not_build_when_closed() throws IOException, InterruptedException, GitAPIException, ExecutionException {
        MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler = new MergeRequestHookTriggerHandlerImpl(Arrays.asList(State.opened, State.updated), false);
        OneShotEvent buildTriggered = doHandle(mergeRequestHookTriggerHandler, State.closed);

        assertThat(buildTriggered.isSignaled(), is(false));
    }

    private OneShotEvent doHandle(MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler, State state) throws GitAPIException, IOException, InterruptedException {
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
                    .withState(state)
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
        return buildTriggered;
    }

}
