package com.dabsquared.gitlabjenkins.publisher;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Awardable;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Robin Müller
 */
public class GitLabVotePublisher extends MergeRequestNotifier {
    private static final Logger LOGGER = Logger.getLogger(GitLabVotePublisher.class.getName());

    @DataBoundConstructor
    public GitLabVotePublisher() {}

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
        boolean alreadyAwarded = false;
        try {
            Integer userId = client.getCurrentUser().getId();
            for (Awardable award : client.getMergeRequestEmoji(mergeRequest)) {
                if (award.getName().equals(getResultIcon(!isSuccessful(build.getResult())))) {
                    if (award.getUser().getId().equals(userId)) {
                        client.deleteMergeRequestEmoji(mergeRequest, award.getId());
                    }
                } else if (award.getName().equals(getResultIcon(isSuccessful(build.getResult())))) {
                    if (award.getUser().getId().equals(userId)) {
                        alreadyAwarded = true;
                    }
                }
            }
        } catch (WebApplicationException | ProcessingException e) {
            listener.getLogger()
                    .printf(
                            "Failed to remove vote on Merge Request for project '%s': %s%n",
                            mergeRequest.getProjectId(), e.getMessage());
            LOGGER.log(
                    Level.SEVERE,
                    "Failed to remove vote on Merge Request for project '%s'".formatted(mergeRequest.getProjectId()),
                    e);
        }

        try {
            if (!alreadyAwarded) {
                client.awardMergeRequestEmoji(mergeRequest, getResultIcon(build.getResult()));
            }
        } catch (NotFoundException e) {
            String message = String.format(
                    "Failed to add vote on Merge Request for project '%s'%n"
                            + "Got unexpected 404, are you using the wrong API version or trying to vote on your own merge request?",
                    mergeRequest.getProjectId());
            listener.getLogger().println(message);
            LOGGER.log(Level.WARNING, message, e);
        } catch (WebApplicationException | ProcessingException e) {
            listener.getLogger()
                    .printf(
                            "Failed to add vote on Merge Request for project '%s': %s%n",
                            mergeRequest.getProjectId(), e.getMessage());
            LOGGER.log(
                    Level.SEVERE,
                    "Failed to add vote on Merge Request for project '%s'".formatted(mergeRequest.getProjectId()),
                    e);
        }
    }

    private boolean isSuccessful(Result result) {
        if (result == Result.SUCCESS) {
            return true;
        } else {
            return false;
        }
    }

    private String getResultIcon(Result result) {
        return getResultIcon(isSuccessful(result));
    }

    private String getResultIcon(boolean success) {
        if (success) {
            return "thumbsup";
        } else {
            return "thumbsdown";
        }
    }
}
