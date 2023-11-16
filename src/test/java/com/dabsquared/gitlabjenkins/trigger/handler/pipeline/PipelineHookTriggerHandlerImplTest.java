package com.dabsquared.gitlabjenkins.trigger.handler.pipeline;

import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.PipelineEventObjectAttributesBuilder.pipelineEventObjectAttributes;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.PipelineHookBuilder.pipelineHook;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.ProjectBuilder.project;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.RepositoryBuilder.repository;
import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterFactory.newBranchFilter;
import static com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilterFactory.newMergeRequestLabelFilter;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.PipelineHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.User;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.OneShotEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
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
public class PipelineHookTriggerHandlerImplTest {

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private PipelineHookTriggerHandler pipelineHookTriggerHandler;
    private PipelineHook pipelineHook;

    @Before
    public void setup() throws IOException, GitAPIException {

        List<String> allowedStates = new ArrayList<>();
        allowedStates.add("success");

        User user = new User();
        user.setName("test");
        user.setId(1);

        Git.init().setDirectory(tmp.getRoot()).call();
        tmp.newFile("test");
        Git git = Git.open(tmp.getRoot());
        git.add().addFilepattern("test");
        git.commit().setSign(false).setMessage("test").call();
        ObjectId head = git.getRepository().resolve(Constants.HEAD);

        pipelineHookTriggerHandler = new PipelineHookTriggerHandlerImpl(allowedStates);
        pipelineHook = pipelineHook()
                .withUser(user)
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
                        .withId(1)
                        .build())
                .withObjectAttributes(pipelineEventObjectAttributes()
                        .withId(1)
                        .withStatus("success")
                        .withSha("bcbb5ec396a2c0f828686f14fac9b80b780504f2")
                        .withStages(new ArrayList<String>())
                        .withRef("refs/heads/" + git.nameRev().add(head).call().get(head))
                        .build())
                .build();
        git.close();
    }

    @Test
    /**
     * always triggers since pipeline events do not contain ci skip message
     */
    public void pipeline_ciSkip() throws Exception {
        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
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
        pipelineHookTriggerHandler.handle(
                project,
                pipelineHook,
                true,
                newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    public void pipeline_build() throws Exception {

        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
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

        pipelineHookTriggerHandler.handle(
                project,
                pipelineHook,
                false,
                newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }
}
