package com.dabsquared.gitlabjenkins.trigger.handler.note;

import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestLabelBuilder.mergeRequestLabel;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestObjectAttributesBuilder.mergeRequestObjectAttributes;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.NoteHookBuilder.noteHook;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.NoteObjectAttributesBuilder.noteObjectAttributes;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.ProjectBuilder.project;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.UserBuilder.user;
import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilterFactory.newMergeRequestLabelFilter;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterFactory;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.GitSCM;
import hudson.util.OneShotEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
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
 * @author Nikolay Ustinov
 */
@WithJenkins
class NoteHookTriggerHandlerImplTest {

    private static JenkinsRule jenkins;

    @TempDir
    private File tmp;

    private NoteHookTriggerHandler noteHookTriggerHandler;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @BeforeEach
    void setUp() {
        noteHookTriggerHandler = new NoteHookTriggerHandlerImpl("ci-run");
    }

    @Test
    void note_ciSkip() throws Exception {
        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                EnvVars env = build.getEnvironment(listener);
                assertNull(env.get("gitlabMergeRequestLabels"));
                buildTriggered.signal();
                return true;
            }
        });
        Date currentDate = new Date();
        project.setQuietPeriod(0);
        noteHookTriggerHandler.handle(
                project,
                noteHook()
                        .withObjectAttributes(noteObjectAttributes()
                                .withId(1L)
                                .withNote("ci-run")
                                .withAuthorId(1)
                                .withProjectId(1)
                                .withCreatedAt(currentDate)
                                .withUpdatedAt(currentDate)
                                .withUrl("https://gitlab.org/test/merge_requests/1#note_1")
                                .build())
                        .withMergeRequest(mergeRequestObjectAttributes()
                                .withDescription("[ci-skip]")
                                .build())
                        .build(),
                true,
                BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    void note_build() throws Exception {
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
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                EnvVars env = build.getEnvironment(listener);
                assertEquals("bugfix,help needed", env.get("gitlabMergeRequestLabels"));
                buildTriggered.signal();
                return true;
            }
        });
        Date currentDate = new Date();
        project.setQuietPeriod(0);
        noteHookTriggerHandler.handle(
                project,
                noteHook()
                        .withObjectAttributes(noteObjectAttributes()
                                .withId(1L)
                                .withNote("ci-run")
                                .withAuthorId(1)
                                .withProjectId(1)
                                .withCreatedAt(currentDate)
                                .withUpdatedAt(currentDate)
                                .withUrl("https://gitlab.org/test/merge_requests/1#note_1")
                                .build())
                        .withMergeRequest(mergeRequestObjectAttributes()
                                .withTargetBranch("refs/heads/"
                                        + git.nameRev().add(head).call().get(head))
                                .withState(State.opened)
                                .withIid(1)
                                .withTitle("test")
                                .withTargetProjectId(1)
                                .withSourceProjectId(1)
                                .withSourceBranch("feature")
                                .withTargetBranch("master")
                                .withLabels(Arrays.asList(
                                        mergeRequestLabel()
                                                .withId(3)
                                                .withTitle("bugfix")
                                                .withColor("#009966")
                                                .withProjectId(1)
                                                .withCreatedAt(currentDate)
                                                .withUpdatedAt(currentDate)
                                                .withTemplate(false)
                                                .withDescription(null)
                                                .withType("ProjectLabel")
                                                .withGroupId(null)
                                                .build(),
                                        mergeRequestLabel()
                                                .withId(4)
                                                .withTitle("help needed")
                                                .withColor("#FF0000")
                                                .withProjectId(1)
                                                .withCreatedAt(currentDate)
                                                .withUpdatedAt(currentDate)
                                                .withTemplate(false)
                                                .withDescription(null)
                                                .withType("ProjectLabel")
                                                .withGroupId(null)
                                                .build()))
                                .withLastCommit(commit().withAuthor(
                                                user().withName("test").build())
                                        .withId(commit.getName())
                                        .build())
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
                                        .withWebUrl("https://gitlab.org/test.git")
                                        .build())
                                .build())
                        .build(),
                true,
                BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
    }
}
