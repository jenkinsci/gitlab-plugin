package com.dabsquared.gitlabjenkins.testing.integration;


import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Pipeline;
import com.dabsquared.gitlabjenkins.publisher.GitLabCommitStatusPublisher;
import com.dabsquared.gitlabjenkins.testing.gitlab.rule.GitLabRule;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import hudson.util.OneShotEvent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SleepBuilder;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.TestNotifier;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

import static com.dabsquared.gitlabjenkins.builder.generated.GitLabPushTriggerBuilder.gitLabPushTrigger;
import static com.dabsquared.gitlabjenkins.testing.gitlab.rule.builder.generated.ProjectRequestBuilder.projectRequest;
import static java.lang.Integer.parseInt;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Robin Müller
 */
public class GitLabIT {
    private static final String GITLAB_URL = "http://localhost:" + System.getProperty("gitlab.http.port", "10080");

    @Rule
    public GitLabRule gitlab = new GitLabRule(GITLAB_URL, parseInt(System.getProperty("postgres.port", "5432")));

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void buildOnPush() throws IOException, InterruptedException, GitAPIException {
        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject("test");
        GitLabPushTrigger trigger = gitLabPushTrigger().withTriggerOnPush(true).withBranchFilterType(BranchFilterType.All).build();
        project.addTrigger(trigger);
        trigger.start(project, true);
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);

        createGitLabProject(false,true, true, false);

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
    }


    @Test
    public void buildOnMergeRequest() throws IOException, InterruptedException, GitAPIException {
        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject("test");
        GitLabPushTrigger trigger = gitLabPushTrigger().withTriggerOnMergeRequest(true).withBranchFilterType(BranchFilterType.All).build();
        project.addTrigger(trigger);
        trigger.start(project, true);
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);

        Pair<Integer, String> gitlabData = createGitLabProject(true,false, false, true);

        gitlab.createMergeRequest(gitlabData.getLeft(), "feature", "master", "Merge feature branch to master.");

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void buildOnNote() throws IOException, InterruptedException, GitAPIException {
        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject("test");
        GitLabPushTrigger trigger = gitLabPushTrigger()
            .withTriggerOnNoteRequest(true)
            .withNoteRegex(".*test.*")
            .withBranchFilterType(BranchFilterType.All).build();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);

        Pair<Integer, String> gitlabData = createGitLabProject(true, false, true, false);

        // create merge-request
        MergeRequest mr = gitlab.createMergeRequest(gitlabData.getLeft(), "feature", "master", "Merge feature branch to master.");

        // add trigger after push/merge-request so it may only receive the note-hook
        project.addTrigger(trigger);
        trigger.start(project, true);

        gitlab.createMergeRequestNote(mr, "this is a test note");

        buildTriggered.block(20000);
        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void reportBuildStatus() throws IOException, InterruptedException, GitAPIException {
        final OneShotEvent buildTriggered = new OneShotEvent();
        final OneShotEvent buildReported = new OneShotEvent();

        GitLabPushTrigger trigger = gitLabPushTrigger()
            .withTriggerOnPush(true)
            .withBranchFilterType(BranchFilterType.All)
            .build();

        FreeStyleProject project = jenkins.createFreeStyleProject("test");
        project.addProperty(gitlab.createGitLabConnectionProperty());
        project.addTrigger(trigger);

        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                buildTriggered.signal();
                return true;
            }
        });
        project.getBuildersList().add(new SleepBuilder(20000));

        DescribableList<Publisher, Descriptor<Publisher>> publishers = project.getPublishersList();
        publishers.add(new GitLabCommitStatusPublisher("integration-test", false));
        publishers.add(new TestNotifier() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                buildReported.signal();
                return true;
            }
        });
        trigger.start(project, true);
        project.setQuietPeriod(0);


        Pair<Integer, String> gitlabData = createGitLabProject(false,true, true, false);
        assertThat(gitlab.getPipelines(gitlabData.getLeft()), empty());

        buildTriggered.block(20000);
        assertThat(buildTriggered.isSignaled(), is(true));
        assertPipelineStatus(gitlabData, "running");

        buildReported.block(40000);
        assertThat(buildReported.isSignaled(), is(true));

        Thread.sleep(5000); // wait for gitlab to update
        assertPipelineStatus(gitlabData, "success");
    }

    private void assertPipelineStatus(Pair<Integer, String> gitlabData, String status) {
        List<Pipeline> pipelines = gitlab.getPipelines(gitlabData.getLeft());
        assertThat(pipelines, hasSize(1));

        Pipeline pipeline = pipelines.get(0);
        assertEquals(gitlabData.getRight(), pipeline.getSha());
        assertEquals(status, pipeline.getStatus());
    }

    private Pair<Integer, String> createGitLabProject(boolean addFeatureBranch, boolean withPushHook, boolean withNoteHook, boolean withMergeRequestHook) throws IOException, GitAPIException {
        // check for clean slate
        assertTrue(gitlab.getProjectIds().isEmpty());

        String url = gitlab.createProject(projectRequest()
            .withName("test")
            .withWebHookUrl("http://" + getDocker0Ip() + ":" + jenkins.getURL().getPort() + "/jenkins/project/test")
            .withPushHook(withPushHook)
            .withNoteHook(withNoteHook)
            .withMergeRequestHook(withMergeRequestHook)
            .build());

        String sha = initGitLabProject(url, addFeatureBranch);

        // Fix: Hack to get the project id
        // A preferable approach would be here to get the target project by name using getProject function of the
        // GitLabRule and to use the id from the project instance. However, due to a bug in GitLab (tested on 8.6.1)
        // retrieving the project by name is not properly working.
        // (see issue https://github.com/gitlabhq/gitlabhq/issues/4921).
        // Once the issue is resolved, replace this implementation.
        List<String> projectIds = gitlab.getProjectIds();
        assertSame(projectIds.size(), 1);
        return new ImmutablePair<>(parseInt(projectIds.get(0)), sha);
    }

    private String initGitLabProject(String url, boolean addFeatureBranch) throws GitAPIException, IOException {
        // Setup git repository
        Git.init().setDirectory(tmp.getRoot()).call();
        Git git = Git.open(tmp.getRoot());
        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", "origin", "url", url);
        config.save();

        // Setup remote master branch
        tmp.newFile("test");
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setMessage("test").call();
        git.push()
            .setRemote("origin").add("master")
            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitlab.getUsername(), gitlab.getPassword()))
            .call();

        if (addFeatureBranch) {
            // Setup remote feature branch
            git.checkout().setName("feature").setCreateBranch(true).call();
            tmp.newFile("feature");
            commit = git.commit().setMessage("feature").call();
            git.push().setRemote("origin").add("feature").setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitlab.getUsername(), gitlab.getPassword()))
                .call();
        }

        return commit.getName();
    }

    private String getDocker0Ip() {
        try {
            Enumeration<InetAddress> docker0Addresses = NetworkInterface.getByName("docker0").getInetAddresses();
            while (docker0Addresses.hasMoreElements()) {
                InetAddress inetAddress = docker0Addresses.nextElement();
                if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                    return inetAddress.getHostAddress();
                }
            }
            throw new RuntimeException("docker0 not available");
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }
}
