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
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AwardEmoji;
import org.gitlab4j.api.models.MergeRequest;
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
    protected void perform(Run<?, ?> build, TaskListener listener, GitLabApi client, MergeRequest mergeRequest) {
        boolean alreadyAwarded = false;
        try {
            Long userId = client.getUserApi().getCurrentUser().getId();
            for (AwardEmoji awardEmoji : client.getAwardEmojiApi()
                    .getMergeRequestAwardEmojis(mergeRequest.getProjectId(), mergeRequest.getIid())) {
                if (awardEmoji.getName().equals(getResultIcon(!isSuccessful(build.getResult())))) {
                    if (awardEmoji.getUser().getId().equals(userId)) {
                        client.getAwardEmojiApi()
                                .deleteMergeRequestAwardEmoji(
                                        mergeRequest.getProjectId(), mergeRequest.getIid(), awardEmoji.getId());
                    }
                } else if (awardEmoji.getName().equals(getResultIcon(isSuccessful(build.getResult())))) {
                    if (awardEmoji.getUser().getId().equals(userId)) {
                        alreadyAwarded = true;
                    }
                }
            }
        } catch (WebApplicationException | GitLabApiException e) {
            listener.getLogger()
                    .printf(
                            "Failed to remove vote on Merge Request for project '%s': %s%n",
                            mergeRequest.getProjectId(), e.getMessage());
            LOGGER.log(
                    Level.SEVERE,
                    String.format(
                            "Failed to remove vote on Merge Request for project '%s'", mergeRequest.getProjectId()),
                    e);
        }

        try {
            if (!alreadyAwarded) {
                client.getAwardEmojiApi()
                        .addMergeRequestAwardEmoji(
                                mergeRequest.getProjectId(), mergeRequest.getIid(), getResultIcon(build.getResult()));
            }
        } catch (NotFoundException | GitLabApiException e) {
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
                    String.format("Failed to add vote on Merge Request for project '%s'", mergeRequest.getProjectId()),
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
