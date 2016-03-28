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

import static com.dabsquared.gitlabjenkins.builder.generated.GitLabPushTriggerBuilder.gitLabPushTrigger;
import static com.dabsquared.gitlabjenkins.testing.gitlab.rule.builder.generated.ProjectRequestBuilder.projectRequest;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
        String httpUrl = gitlab.createProject(projectRequest()
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
