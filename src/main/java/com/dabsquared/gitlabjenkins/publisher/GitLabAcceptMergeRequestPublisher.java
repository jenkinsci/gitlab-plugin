package com.dabsquared.gitlabjenkins.publisher;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.WebApplicationException;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.MergeRequest;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * @author Robin Müller
 */
public class GitLabAcceptMergeRequestPublisher extends MergeRequestNotifier {
    private static final Logger LOGGER = Logger.getLogger(GitLabAcceptMergeRequestPublisher.class.getName());

    private Boolean deleteSourceBranch;

    @DataBoundConstructor
    public GitLabAcceptMergeRequestPublisher() {}

    @DataBoundSetter
    public void setDeleteSourceBranch(boolean deleteSourceBranch) {
        this.deleteSourceBranch = deleteSourceBranch;
    }

    public boolean isDeleteSourceBranch() {
        return deleteSourceBranch;
    }

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
            return Messages.GitLabAcceptMergeRequestPublisher_DisplayName();
        }
    }

    @Override
    protected void perform(Run<?, ?> build, TaskListener listener, GitLabApi client, MergeRequest mergeRequest) {
        try {
            if (build.getResult() == Result.SUCCESS) {
                client.getMergeRequestApi()
                        .acceptMergeRequest(
                                mergeRequest,
                                mergeRequest.getIid(),
                                "Merge Request accepted by jenkins build success",
                                isDeleteSourceBranch(),
                                true);
            }
        } catch (WebApplicationException | GitLabApiException e) {
            listener.getLogger()
                    .printf(
                            "Failed to accept merge request for project '%s': %s%n",
                            mergeRequest.getProjectId(), e.getMessage());
            LOGGER.log(
                    Level.SEVERE,
                    String.format("Failed to accept merge request for project '%s'", mergeRequest.getProjectId()),
                    e);
        }
    }
}
