package com.dabsquared.gitlabjenkins.publisher;


import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Robin Müller
 */
public class GitLabVotePublisher extends MergeRequestNotifier {
    private static final Logger LOGGER = Logger.getLogger(GitLabVotePublisher.class.getName());

    @DataBoundConstructor
    public GitLabVotePublisher() { }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
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

    @Override
    protected void perform(Run<?, ?> build, TaskListener listener, GitLabClient client, MergeRequest mergeRequest) {
        try {
            client.createMergeRequestNote(mergeRequest, getResultIcon(build.getResult()));
        } catch (WebApplicationException | ProcessingException e) {
            listener.getLogger().printf("Failed to add vote on Merge Request for project '%s': %s%n", mergeRequest.getProjectId(), e.getMessage());
            LOGGER.log(Level.SEVERE, String.format("Failed to add vote on Merge Request for project '%s'", mergeRequest.getProjectId()), e);
        }
    }

    private String getResultIcon(Result result) {
        if (result == Result.SUCCESS) {
            return ":+1:";
        } else {
            return ":-1:";
        }
    }
}
