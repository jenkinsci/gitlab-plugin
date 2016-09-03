package com.dabsquared.gitlabjenkins.publisher;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Robin Müller
 */
public class GitLabVotePublisher extends Notifier {
    private static final Logger LOGGER = Logger.getLogger(GitLabVotePublisher.class.getName());

    @DataBoundConstructor
    public GitLabVotePublisher() { }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        addVoteOnMergeRequest(build, listener);
        return true;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.GitLabVotePublisher_DisplayName();
        }
    }

    private void addVoteOnMergeRequest(Run<?, ?> build, TaskListener listener) {
        String projectId = getProjectId(build);
        Integer mergeRequestId = getMergeRequestId(build);
        if (projectId != null && mergeRequestId != null) {
            try {
                GitLabApi client = getClient(build);
                if (client == null) {
                    listener.getLogger().println("No GitLab connection configured");
                } else {
                    client.createMergeRequestNote(projectId, mergeRequestId, getResultIcon(build.getResult()));
                }
            } catch (WebApplicationException | ProcessingException e) {
                listener.getLogger().printf("Failed to add vote on Merge Request for project '%s': %s%n", projectId, e.getMessage());
                LOGGER.log(Level.SEVERE, String.format("Failed to add vote on Merge Request for project '%s'", projectId), e);
            }
        }
    }

    String getProjectId(Run<?, ?> build) {
        GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);
        return cause == null ? null : cause.getData().getTargetProjectId().toString();
    }

    Integer getMergeRequestId(Run<?, ?> build) {
        GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);
        return cause == null ? null : cause.getData().getMergeRequestId();
    }

    private String getResultIcon(Result result) {
        if (result == Result.SUCCESS) {
            return ":+1:";
        } else {
            return ":-1:";
        }
    }

    private static GitLabApi getClient(Run<?, ?> build) {
        GitLabConnectionProperty connectionProperty = build.getParent().getProperty(GitLabConnectionProperty.class);
        if (connectionProperty != null) {
            return connectionProperty.getClient();
        }
        return null;
    }
}
