package com.dabsquared.gitlabjenkins.testing.integration;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.testing.gitlab.rule.GitLabRule;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.util.OneShotEvent;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

import static com.dabsquared.gitlabjenkins.builder.generated.GitLabPushTriggerBuilder.gitLabPushTrigger;
import static com.dabsquared.gitlabjenkins.testing.gitlab.rule.builder.generated.ProjectRequestBuilder.projectRequest;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Robin Müller
 */
public class GitLabIT {

    @Rule
    public GitLabRule gitlab = new GitLabRule("http://localhost:" + System.getProperty("gitlab.http.port", "10080"),
        Integer.parseInt(System.getProperty("postgres.port", "5432")));

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void buildOnPush() throws IOException, InterruptedException, GitAPIException {
        final String httpUrl = gitlab.createProject(projectRequest()
            .withName("test")
            .withWebHookUrl("http://" + getDocker0Ip() + ":" + jenkins.getURL().getPort() + "/jenkins/project/test")
            .withPushHook(true)
            .build());

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

        Git.init().setDirectory(tmp.getRoot()).call();
        tmp.newFile("test");
        Git git = Git.open(tmp.getRoot());
        git.add().addFilepattern("test");
        git.commit().setMessage("test").call();
        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", "origin", "url", httpUrl);
        config.save();
        git.push()
            .setRemote("origin").add("master")
            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitlab.getUsername(), gitlab.getPassword()))
            .call();

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
    }

    @Test
    public void buildOnMergeRequest() throws IOException, InterruptedException, GitAPIException {

        // check for clean slate
        assertTrue(gitlab.getProjectIds().isEmpty());

        final String httpUrl = gitlab.createProject(projectRequest()
            .withName("test")
            .withWebHookUrl("http://" + getDocker0Ip() + ":" + jenkins.getURL().getPort() + "/jenkins/project/test")
            .withMergeRequestHook(true)
            .build());

        // Fix: Hack to get the project id
        // A preferable approach would be here to get the target project by name using getProject function of the
        // GitLabRule and to use the id from the project instance. However, due to a bug in GitLab (tested on 8.6.1)
        // retrieving the project by name is not properly working.
        // (see issue https://github.com/gitlabhq/gitlabhq/issues/4921).
        // Once the issue is resolved, replace this implementation.
        final List<String> projectIds = gitlab.getProjectIds();
        assertSame(projectIds.size(), 1);
        final Integer projectId = Integer.parseInt(projectIds.get(0));

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

        // Setup git repository
        Git.init().setDirectory(tmp.getRoot()).call();
        Git git = Git.open(tmp.getRoot());
        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", "origin", "url", httpUrl);
        config.save();

        // Setup remote master branch
        tmp.newFile("test");
        git.add().addFilepattern("test");
        git.commit().setMessage("test").call();
        git.push()
            .setRemote("origin").add("master")
            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitlab.getUsername(), gitlab.getPassword()))
            .call();

        // Setup remote feature branch
        git.checkout().setName("feature").setCreateBranch(true).call();
        tmp.newFile("feature");
        git.commit().setMessage("feature").call();
        git.push().setRemote("origin").add("feature").setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitlab.getUsername(), gitlab.getPassword()))
            .call();

        gitlab.createMergeRequest(projectId, "feature", "master", "Merge feature branch to master.");

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
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
