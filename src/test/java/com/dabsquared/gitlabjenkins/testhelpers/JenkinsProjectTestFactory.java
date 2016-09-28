package com.dabsquared.gitlabjenkins.testhelpers;

import com.dabsquared.gitlabjenkins.trigger.handler.merge.LockWrapper;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.plugins.git.GitSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;

/**
 * Factory to create Jenkins projects suitable for tests. Useful to avoid needless boilerplate.
 *
 * @author benjie332
 */
public class JenkinsProjectTestFactory {

    public ProjectSetupResult createProject(final JenkinsRule jenkins,
                                            final String gitRepoUrl) throws IOException {
        final FreeStyleProject testProject = jenkins.createFreeStyleProject();
        //testProject.setScm(new GitSCM(gitRepoUrl));
        testProject.setQuietPeriod(0);

        final BuildNotifier buildNotifier = addNotifier(testProject);

        return new ProjectSetupResult(testProject, buildNotifier);
    }

    private BuildNotifier addNotifier(Project<FreeStyleProject, hudson.model.FreeStyleBuild> project) throws IOException {
            final BuildNotifier buildNotifier = new BuildNotifier(new LockWrapper());
            project.getBuildersList().add(new TestBuilder() {
                @Override
                public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

                    buildNotifier.addBuild(build);
                    buildNotifier.getLock().signal();
                    return true;
                }
            });
            return buildNotifier;
        }
}
