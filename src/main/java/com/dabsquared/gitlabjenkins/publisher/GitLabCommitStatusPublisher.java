package com.dabsquared.gitlabjenkins.publisher;

import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.util.CommitStatusUpdater;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * @author Robin Müller
 */
public class GitLabCommitStatusPublisher extends Notifier {

    @DataBoundConstructor
    public GitLabCommitStatusPublisher() { }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        CommitStatusUpdater.updateCommitStatus(build, listener, BuildState.running);
        return true;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        Result buildResult = build.getResult();
        if (buildResult == Result.SUCCESS) {
            CommitStatusUpdater.updateCommitStatus(build, listener, BuildState.success);
        } else if (buildResult == Result.ABORTED) {
            CommitStatusUpdater.updateCommitStatus(build, listener, BuildState.canceled);
        } else {
            CommitStatusUpdater.updateCommitStatus(build, listener, BuildState.failed);
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
        return cause == null ? null : cause.getData().getSourceBranch();
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
        GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);
        String sourceRepoSshUrl = cause.getData().getSourceRepoSshUrl();
        String targetRepoSshUrl = cause.getData().getTargetRepoSshUrl();
        if (sourceRepoSshUrl != null) {
            try {
                result.add(ProjectIdUtil.retrieveProjectId(sourceRepoSshUrl));
            } catch (ProjectIdUtil.ProjectIdResolutionException e) {
                // nothing to do
            }
        }
        if (targetRepoSshUrl != null) {
            try {
                result.add(ProjectIdUtil.retrieveProjectId(targetRepoSshUrl));
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
