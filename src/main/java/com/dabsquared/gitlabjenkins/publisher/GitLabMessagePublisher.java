package com.dabsquared.gitlabjenkins.publisher;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Nikolay Ustinov
 */
public class GitLabMessagePublisher extends MergeRequestNotifier {
    private static final Logger LOGGER = Logger.getLogger(GitLabMessagePublisher.class.getName());
    private boolean replaceSuccessNote = false;
    private boolean replaceFailureNote = false;
    private boolean replaceAbortNote = false;
    private String successNoteText;
    private String failureNoteText;
    private String abortNoteText;

    @DataBoundConstructor
    public GitLabMessagePublisher(boolean replaceSuccessNote, boolean replaceFailureNote, boolean replaceAbortNote,
                                  String successNoteText, String failureNoteText, String abortNoteText) {
        this.replaceSuccessNote = replaceSuccessNote;
        this.replaceFailureNote = replaceFailureNote;
        this.replaceAbortNote = replaceAbortNote;
        this.successNoteText = successNoteText;
        this.failureNoteText = failureNoteText;
        this.abortNoteText = abortNoteText;
    }

    public GitLabMessagePublisher() { }

    public boolean getReplaceSuccessNote() {
        return replaceSuccessNote;
    }

    public boolean getReplaceFailureNote() {
        return replaceFailureNote;
    }

    public boolean getReplaceAbortNote() {
        return replaceAbortNote;
    }

    public String getSuccessNoteText() {
        return this.successNoteText == null ? "" : this.successNoteText;
    }

    public String getFailureNoteText() {
        return this.failureNoteText == null ? "" : this.failureNoteText;
    }

    public String getAbortNoteText() {
        return this.abortNoteText == null ? "" : this.abortNoteText;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.GitLabMessagePublisher_DisplayName();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/gitlab-plugin/help/help-messagesOnResult.html";
        }
    }

    @Override
    protected void perform(Run<?, ?> build, TaskListener listener, GitLabApi client, Integer projectId, Integer mergeRequestId) {
        try {
            client.createMergeRequestNote(projectId, mergeRequestId, getNote(build, listener));
        } catch (WebApplicationException | ProcessingException e) {
            listener.getLogger().printf("Failed to add comment on Merge Request for project '%s': %s%n", projectId, e.getMessage());
            LOGGER.log(Level.SEVERE, String.format("Failed to add comment on Merge Request for project '%s'", projectId), e);
        }
    }

    private String getResultIcon(Result result) {
        if (result == Result.SUCCESS) {
            return ":white_check_mark:";
        } else if (result == Result.ABORTED) {
            return ":point_up:";
        } else {
            return ":negative_squared_cross_mark:";
        }
    }

    private static String replaceMacros(Run<?, ?> build, TaskListener listener, String inputString) {
        String returnString = inputString;
        if (build != null && inputString != null) {
            try {
                Map<String, String> messageEnvVars = getEnvVars(build, listener);
                returnString = Util.replaceMacro(inputString, messageEnvVars);

            } catch (Exception e) {
                listener.getLogger().printf("Couldn't replace macros in message: %s%n", e.getMessage());
                LOGGER.log(Level.WARNING, "Couldn't replace macros in message", e);
            }
        }
        return returnString;
    }

    private static Map<String, String> getEnvVars(Run<?, ?> build, TaskListener listener) {
        Map<String, String> messageEnvVars = new HashMap<>();
        if (build != null) {
            messageEnvVars.putAll(build.getCharacteristicEnvVars());
            try {
                messageEnvVars.putAll(build.getEnvironment(listener));
            } catch (Exception e) {
                listener.getLogger().printf("Couldn't get Env Variables: %s%n", e.getMessage());
                LOGGER.log(Level.WARNING, "Couldn't get Env Variables", e);
            }
        }
        return messageEnvVars;
    }

    private String getNote(Run<?, ?> build, TaskListener listener) {
        String message;
        if (this.getReplaceSuccessNote() && build.getResult() == Result.SUCCESS) {
            message = replaceMacros(build, listener, this.getSuccessNoteText());
        } else if (this.getReplaceAbortNote() && build.getResult() == Result.ABORTED) {
            message = replaceMacros(build, listener, this.getAbortNoteText());
        } else if (this.getReplaceFailureNote() && build.getResult() == Result.FAILURE) {
            message = replaceMacros(build, listener, this.getFailureNoteText());
        } else {
            String icon = getResultIcon(build.getResult());
            String buildUrl = Jenkins.getInstance().getRootUrl() + build.getUrl();
            message = MessageFormat.format("{0} Jenkins Build {1}\n\nResults available at: [Jenkins [{2} #{3}]]({4})",
                                           icon, build.getResult().toString(), build.getParent().getDisplayName(), build.getNumber(), buildUrl);
        }
        return message;
    }
}
