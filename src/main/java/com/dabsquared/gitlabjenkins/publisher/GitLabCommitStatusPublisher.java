package com.dabsquared.gitlabjenkins.publisher;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import hudson.Extension;
import hudson.Launcher;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.plugins.git.util.BuildData;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.model.Jenkins;
import org.eclipse.jgit.transport.URIish;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabProject;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * @author Robin Müller
 */
public class GitLabCommitStatusPublisher extends Recorder {

    @DataBoundConstructor
    public GitLabCommitStatusPublisher() { }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        GitlabProject buildProject = retrieveGitlabProject(build, listener);
        if (buildProject != null) {
            String commitHash = getBuildRevision(build);
            updateCommitStatus(build, listener, buildProject, commitHash, "running", getBuildUrl(build));
        }
        return true;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        GitlabProject buildProject = retrieveGitlabProject(build, listener);
        if (buildProject != null) {
            String commitHash = getBuildRevision(build);
            String buildUrl = getBuildUrl(build);
            Result buildResult = build.getResult();
            if (buildResult == Result.SUCCESS) {
                updateCommitStatus(build, listener, buildProject, commitHash, "success", buildUrl);
            } else if (buildResult == Result.ABORTED) {
                updateCommitStatus(build, listener, buildProject, commitHash, "canceled", buildUrl);
            } else {
                updateCommitStatus(build, listener, buildProject, commitHash, "failed", buildUrl);
            }
        }
        return true;
    }

    private String getBuildRevision(AbstractBuild<?, ?> build) {
        return build.getAction(BuildData.class).getLastBuiltRevision().getSha1String();
    }

    private void updateCommitStatus(AbstractBuild<?, ?> build, BuildListener listener, GitlabProject buildProject, String commitHash, String state, String buildUrl) {
        try {
            GitlabAPI client = getClient(build);
            if (client == null) {
                listener.getLogger().println("No GitLab connection configured");
            } else {
                client.createCommitStatus(buildProject, commitHash, state, commitHash, "jenkins", buildUrl, null);
            }
        } catch (IOException e) {
            listener.getLogger().println("Failed to update Gitlab commit status");
        }
    }

    private String getBuildUrl(AbstractBuild<?, ?> build) {
        return Jenkins.getInstance().getRootUrl() + build.getUrl();
    }

    private GitlabAPI getClient(AbstractBuild<?, ?> build) {
        GitLabConnectionProperty connectionProperty = build.getProject().getProperty(GitLabConnectionProperty.class);
        if (connectionProperty != null) {
            return connectionProperty.getClient();
        }
        return null;
    }

    private GitlabProject retrieveGitlabProject(AbstractBuild<?, ?> build, TaskListener listener) {
        GitlabAPI client = getClient(build);
        if (client == null) {
            listener.getLogger().println("No GitLab connection configured");
        } else {
            Set<String> remoteUrls = build.getAction(BuildData.class).getRemoteUrls();
            for (String remoteUrl : remoteUrls) {
                try {
                    try {
                        return client.getProject(retrieveProjectId(remoteUrl));
                    } catch (Throwable e) {
                        listener.getLogger().printf("Failed to retrieve GitLab project for projectId: %s", retrieveProjectId(remoteUrl));
                    }
                } catch (URISyntaxException e) {
                    // nothing to do
                }
            }
        }
        return null;
    }

    private String retrieveProjectId(String remoteUrl) throws URISyntaxException {
        String projectId = new URIish(remoteUrl).getPath();
        if (projectId.startsWith("/")) {
            projectId = projectId.substring(1);
        }
        if (projectId.endsWith(".git")) {
            projectId = projectId.substring(0, projectId.lastIndexOf(".git"));
        }
        return projectId;
    }

    @Initializer(after = InitMilestone.JOB_LOADED)
    public static void migrateJobs() throws IOException {
        DescriptorImpl descriptor = (DescriptorImpl) Jenkins.getInstance().getDescriptor(GitLabCommitStatusPublisher.class);
        if (!descriptor.migrationFinished) {
            for (AbstractProject<?, ?> project : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
                GitLabPushTrigger trigger = project.getTrigger(GitLabPushTrigger.class);
                if (trigger != null && trigger.getAddCiMessage()) {
                    project.getPublishersList().add(new GitLabCommitStatusPublisher());
                    trigger.setAddCiMessage(false);
                    project.save();
                }
            }
            descriptor.migrationFinished = true;
            descriptor.save();
        }
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private boolean migrationFinished = false;

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.GitLabCommitStatusPublisher_DisplayName();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/gitlab-plugin/help/help-gitlab8.1CI.html";
        }
    }
}
