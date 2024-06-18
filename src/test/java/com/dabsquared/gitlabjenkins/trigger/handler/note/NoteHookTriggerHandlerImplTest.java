package com.dabsquared.gitlabjenkins.trigger.handler.note;

import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilterFactory.newMergeRequestLabelFilter;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterFactory;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.GitSCM;
import hudson.util.OneShotEvent;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.gitlab4j.api.Constants.MergeRequestState;
import org.gitlab4j.api.models.Author;
import org.gitlab4j.api.webhook.EventCommit;
import org.gitlab4j.api.webhook.EventProject;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.NoteEvent;
import org.gitlab4j.api.webhook.NoteEvent.ObjectAttributes;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

/**
 * @author Nikolay Ustinov
 */
public class NoteHookTriggerHandlerImplTest {

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private NoteHookTriggerHandler noteHookTriggerHandler;

    @Before
    public void setup() {
        noteHookTriggerHandler = new NoteHookTriggerHandlerImpl("ci-run");
    }

    @Test
    public void note_ciSkip() throws IOException, InterruptedException {
        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                buildTriggered.signal();
                return true;
            }
        });
        Date currentDate = new Date();
        project.setQuietPeriod(0);
        ObjectAttributes noteObjectAttributes = new ObjectAttributes();
        noteObjectAttributes.setId(1L);
        noteObjectAttributes.setNote("ci-run");
        noteObjectAttributes.setAuthorId(1L);
        noteObjectAttributes.setProjectId(1L);
        noteObjectAttributes.setCreatedAt(currentDate);
        noteObjectAttributes.setUpdatedAt(currentDate);
        noteObjectAttributes.setUrl("https://gitlab.org/test/merge_requests/1#note_1");
        NoteEvent noteEvent = new NoteEvent();
        noteEvent.setObjectAttributes(noteObjectAttributes);
        org.gitlab4j.api.webhook.MergeRequestEvent.ObjectAttributes mergeRequestObjectAttributes =
                new org.gitlab4j.api.webhook.MergeRequestEvent.ObjectAttributes();
        mergeRequestObjectAttributes.setDescription("[ci-skip]");
        MergeRequestEvent mergeRequestEvent = new MergeRequestEvent();
        mergeRequestEvent.setObjectAttributes(mergeRequestObjectAttributes);
        noteEvent.setMergeRequest(mergeRequestObjectAttributes);
        noteHookTriggerHandler.handle(
                project,
                noteEvent,
                true,
                BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    public void note_build() throws IOException, InterruptedException, GitAPIException, ExecutionException {
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
        Date currentDate = new Date();
        project.setQuietPeriod(0);
        ObjectAttributes noteObjectAttributes = new ObjectAttributes();
        noteObjectAttributes.setId(1L);
        noteObjectAttributes.setNote("ci-run");
        noteObjectAttributes.setAuthorId(1L);
        noteObjectAttributes.setProjectId(1L);
        noteObjectAttributes.setCreatedAt(currentDate);
        noteObjectAttributes.setUpdatedAt(currentDate);
        noteObjectAttributes.setUrl("https://gitlab.org/test/merge_requests/1#note_1");
        NoteEvent noteEvent = new NoteEvent();
        noteEvent.setObjectAttributes(noteObjectAttributes);
        org.gitlab4j.api.webhook.MergeRequestEvent.ObjectAttributes mergeRequestObjectAttributes =
                new org.gitlab4j.api.webhook.MergeRequestEvent.ObjectAttributes();
        mergeRequestObjectAttributes.setTargetBranch(
                "refs/heads/" + git.nameRev().add(head).call().get(head));
        mergeRequestObjectAttributes.setState((MergeRequestState.OPENED).toString());
        mergeRequestObjectAttributes.setIid(1L);
        mergeRequestObjectAttributes.setTitle("test");
        mergeRequestObjectAttributes.setTargetProjectId(1L);
        mergeRequestObjectAttributes.setSourceProjectId(1L);
        mergeRequestObjectAttributes.setTargetBranch("master");
        mergeRequestObjectAttributes.setSourceBranch("feature");
        Author author = new Author();
        author.setName("test");
        EventCommit lastCommit = new EventCommit();
        lastCommit.setAuthor(author);
        lastCommit.setId(commit.getId().getName());
        mergeRequestObjectAttributes.setLastCommit(lastCommit);
        EventProject eventProject = new EventProject();
        eventProject.setName("test");
        eventProject.setNamespace("test-namespace");
        eventProject.setHomepage("https://gitlab.org/test");
        eventProject.setUrl("git@gitlab.org:test.git");
        eventProject.setSshUrl("git@gitlab.org:test.git");
        eventProject.setHttpUrl("https://gitlab.org/test.git");
        mergeRequestObjectAttributes.setSource(eventProject);
        mergeRequestObjectAttributes.setTarget(eventProject);
        MergeRequestEvent mergeRequestEvent = new MergeRequestEvent();
        mergeRequestEvent.setObjectAttributes(mergeRequestObjectAttributes);
        noteEvent.setMergeRequest(mergeRequestObjectAttributes);
        noteEvent.setProject(eventProject);
        noteHookTriggerHandler.handle(
                project,
                noteEvent,
                true,
                BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newMergeRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
    }
}
