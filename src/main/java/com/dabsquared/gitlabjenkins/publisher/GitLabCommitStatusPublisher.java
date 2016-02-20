package com.dabsquared.gitlabjenkins.publisher;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import hudson.Extension;
import hudson.Launcher;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.git.util.BuildData;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.model.Jenkins;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Robin Müller
 */
public class GitLabCommitStatusPublisher extends Recorder {

    private final static Logger LOGGER = Logger.getLogger(GitLabCommitStatusPublisher.class.getName());

    @DataBoundConstructor
    public GitLabCommitStatusPublisher() { }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        String commitHash = getBuildRevision(build);
        updateCommitStatus(build, listener, BuildState.running, commitHash, getBuildUrl(build));
        return true;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        String commitHash = getBuildRevision(build);
        String buildUrl = getBuildUrl(build);
        Result buildResult = build.getResult();
        if (buildResult == Result.SUCCESS) {
            updateCommitStatus(build, listener, BuildState.success, commitHash, buildUrl);
        } else if (buildResult == Result.ABORTED) {
            updateCommitStatus(build, listener, BuildState.canceled, commitHash, buildUrl);
        } else {
            updateCommitStatus(build, listener, BuildState.failed, commitHash, buildUrl);
        }
        return true;
    }

    private String getBuildRevision(AbstractBuild<?, ?> build) {
        return build.getAction(BuildData.class).getLastBuiltRevision().getSha1String();
    }

    private void updateCommitStatus(AbstractBuild<?, ?> build, BuildListener listener, BuildState state, String commitHash, String buildUrl) {
        for (String gitlabProjectId : retrieveGitlabProjectIds(build)) {
            try {
                GitLabApi client = getClient(build);
                if (client == null) {
                    listener.getLogger().println("No GitLab connection configured");
                } else if (existsCommit(client, gitlabProjectId, commitHash)) {
                    client.changeBuildStatus(gitlabProjectId, commitHash, state, commitHash, "jenkins", buildUrl, null);
                }
            } catch (WebApplicationException e) {
                listener.getLogger().printf("Failed to update Gitlab commit status for project '%s': %s%n", gitlabProjectId, e.getMessage());
                LOGGER.log(Level.SEVERE, String.format("Failed to update Gitlab commit status for project '%s'", gitlabProjectId), e);
            }
        }
    }

    private boolean existsCommit(GitLabApi client, String gitlabProjectId, String commitHash) {
        try {
            client.headCommit(gitlabProjectId, commitHash);
            return true;
        } catch (NotFoundException e) {
            LOGGER.log(Level.FINE, String.format("Project (%s) and commit (%s) combination not found", gitlabProjectId, commitHash));
            return false;
        }
    }

    private String getBuildUrl(AbstractBuild<?, ?> build) {
        return Jenkins.getInstance().getRootUrl() + build.getUrl();
    }

    private GitLabApi getClient(AbstractBuild<?, ?> build) {
        GitLabConnectionProperty connectionProperty = build.getProject().getProperty(GitLabConnectionProperty.class);
        if (connectionProperty != null) {
            return connectionProperty.getClient();
        }
        return null;
    }

    private List<String> retrieveGitlabProjectIds(AbstractBuild<?, ?> build) {
        List<String> result = new ArrayList<String>();
        for (String remoteUrl : build.getAction(BuildData.class).getRemoteUrls()) {
            try {
                result.add(retrieveProjectId(remoteUrl));
            } catch (URISyntaxException e) {
                // nothing to do
            }
        }
        return result;
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
