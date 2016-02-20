package com.dabsquared.gitlabjenkins.publisher;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.util.ProjectIdUtil;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.git.util.BuildData;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
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
public class GitLabCommitStatusPublisher extends Notifier {

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
                    client.changeBuildStatus(gitlabProjectId, commitHash, state, getBuildBranch(build), "jenkins", buildUrl, null);
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

    private String getBuildBranch(AbstractBuild<?, ?> build) {
        GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);
        return cause == null ? null : cause.getSourceBranch();
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
        List<String> result = new ArrayList<>();
        for (String remoteUrl : build.getAction(BuildData.class).getRemoteUrls()) {
            try {
                result.add(ProjectIdUtil.retrieveProjectId(remoteUrl));
            } catch (ProjectIdUtil.ProjectIdResolutionException e) {
                // nothing to do
            }
        }
        return result;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

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
