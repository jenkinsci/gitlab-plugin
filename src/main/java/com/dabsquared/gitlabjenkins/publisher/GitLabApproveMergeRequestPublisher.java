package com.dabsquared.gitlabjenkins.publisher;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitLabApproveMergeRequestPublisher extends MergeRequestNotifier {
    private static final Logger LOGGER = Logger.getLogger(GitLabApproveMergeRequestPublisher.class.getName());

    private final boolean approveUnstableBuilds;

    @DataBoundConstructor
    public GitLabApproveMergeRequestPublisher(boolean approveUnstableBuilds) {
        this.approveUnstableBuilds = approveUnstableBuilds;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/gitlab-plugin/help/help-approve-gitlab-mergerequest.html";
        }

        @Override
        public String getDisplayName() {
            return Messages.GitLabApproveMergeRequestPublisher_DisplayName();
        }
    }

    public boolean isApproveUnstableBuilds() {
        return approveUnstableBuilds;
    }

    @Override
    protected void perform(Run<?, ?> build, TaskListener listener, GitLabClient client, MergeRequest mergeRequest) {
        try {
            Result buildResult = build.getResult();
            if (build.getResult() == Result.SUCCESS || (buildResult == Result.UNSTABLE && isApproveUnstableBuilds())) {
                client.approveMergeRequest(mergeRequest);
            } else {
                client.unapproveMergeRequest(mergeRequest);
            }
        } catch (NotFoundException e) {
            String message = String.format(
                "Failed to approve/unapprove merge request '%s' for project '%s'.\n"
                    + "Got unexpected 404. Does your GitLab edition or GitLab.com tier really support approvals, and are you are an eligible approver for this merge request?", mergeRequest.getIid(), mergeRequest.getProjectId());
            listener.getLogger().printf(message);
            LOGGER.log(Level.WARNING, message, e);
        } catch (NotAuthorizedException e) {
            String message = String.format(
                "Failed to approve/unapprove merge request '%s' for project '%s'.\n"
                + "Got unexpected 401, are you using the wrong credentials?", mergeRequest.getIid(), mergeRequest.getProjectId());
            listener.getLogger().printf(message);
            LOGGER.log(Level.WARNING, message, e);
        } catch (WebApplicationException | ProcessingException e) {
            listener.getLogger().printf("Failed to approve/unapprove merge request for project '%s': %s%n", mergeRequest.getProjectId(), e.getMessage());
            LOGGER.log(Level.SEVERE, String.format("Failed to approve/unapprove merge request for project '%s'", mergeRequest.getProjectId()), e);
        }
    }

}
