package com.dabsquared.gitlabjenkins.trigger.handler.pipeline;

import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterFactory.newBranchFilter;
import static com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilterFactory.newMergeRequestLabelFilter;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
import org.gitlab4j.api.models.User;
import org.gitlab4j.api.webhook.EventProject;
import org.gitlab4j.api.webhook.EventRepository;
import org.gitlab4j.api.webhook.PipelineEvent;
import org.gitlab4j.api.webhook.PipelineEvent.ObjectAttributes;
import org.junit.After;
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
    private PipelineEvent pipelineEvent;

    @Before
    public void setup() throws IOException, GitAPIException {

        List<String> allowedStates = new ArrayList<>();
        allowedStates.add("SUCCESS");

        User user = new User();
        user.setName("test");
        user.setId(1L);

        Git.init().setDirectory(tmp.getRoot()).call();
        tmp.newFile("test");
        Git git = Git.open(tmp.getRoot());
        git.add().addFilepattern("test");
        git.commit().setSign(false).setMessage("test").call();
        ObjectId head = git.getRepository().resolve(Constants.HEAD);

        pipelineHookTriggerHandler = new PipelineHookTriggerHandlerImpl(allowedStates);
        ObjectAttributes objectAttributes = new ObjectAttributes();
        objectAttributes.setId(1L);
        objectAttributes.setStatus("SUCCESS");
        objectAttributes.setSha("bcbb5ec396a2c0f828686f14fac9b80b780504f2");
        objectAttributes.setStages(new ArrayList<String>());
        objectAttributes.setRef("refs/heads/" + git.nameRev().add(head).call().get(head));
        EventProject project = new EventProject();
        project.setNamespace("test-namespace");
        project.setWebUrl("https://gitlab.org/test");
        project.setId(1L);
        EventRepository repository = new EventRepository();
        repository.setName("test");
        repository.setHomepage("https://gitlab.org/test");
        repository.setUrl("git@gitlab.org:test.git");
        repository.setGit_http_url("https://gitlab.org/test.git");
        repository.setGit_ssh_url("git@gitlab.org:test.git");
        // not able to set Repository
        pipelineEvent = new PipelineEvent();
        pipelineEvent.setUser(user);
        pipelineEvent.setObjectAttributes(objectAttributes);
        pipelineEvent.setProject(project);
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
                pipelineEvent,
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
                buildTriggered.signal();
                buildHolder.set((FreeStyleBuild) build);
                return true;
            }
        });
        project.setQuietPeriod(0);

        pipelineHookTriggerHandler.handle(
                project,
                pipelineEvent,
                false,
                newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }
}
