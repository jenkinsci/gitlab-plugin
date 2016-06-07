package com.dabsquared.gitlabjenkins.publisher;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Nikolay Ustinov
 */
public class GitLabMessagePublisher extends Notifier {
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

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        addNoteOnMergeRequest(build, listener);
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
            return Messages.GitLabMessagePublisher_DisplayName();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/gitlab-plugin/help/help-messagesOnResult.html";
        }
    }

    private void addNoteOnMergeRequest(Run<?, ?> build, TaskListener listener) {
        String projectId = getProjectId(build);
        Integer mergeRequestId = getMergeRequestId(build);
        if (projectId != null && mergeRequestId != null) {
            try {
                GitLabApi client = getClient(build);
                if (client == null) {
                    LOGGER.log(Level.WARNING, "No GitLab connection configured");
                } else if (existsMergeRequest(client, projectId, mergeRequestId)) {
                    client.createMergeRequestNote(projectId, mergeRequestId, getNote(build, listener));
                }
            } catch (WebApplicationException e) {
                LOGGER.log(Level.SEVERE, String.format("Failed to update Gitlab commit status for project '%s'", projectId), e);
            }
        }
    }

    private static boolean existsMergeRequest(GitLabApi client, String projectId, Integer mergeRequestId) {
        try {
            client.getMergeRequest(projectId, mergeRequestId);
            return true;
        } catch (NotFoundException e) {
            LOGGER.log(Level.FINE, String.format("Project (%s) and merge request (%s) combination not found", projectId, mergeRequestId));
            return false;
        }
    }

    public String getProjectId(Run<?, ?> build) {
        GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);
        return cause == null ? null : cause.getData().getProjectId().toString();
    }

    public Integer getMergeRequestId(Run<?, ?> build) {
        GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);
        return cause == null ? null : cause.getData().getMergeRequestId();
    }

    private String getResultIcon(Result result) {
        if (result == Result.SUCCESS) {
            return ":+1:";
        } else if (result == Result.ABORTED) {
            return ":point_up:";
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

    public static String replaceMacros(Run<?, ?> build, TaskListener listener, String inputString) {
        String returnString = inputString;
        if (build != null && inputString != null) {
            try {
                Map<String, String> messageEnvVars = getEnvVars(build, listener);
                returnString = Util.replaceMacro(inputString, messageEnvVars);

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Couldn't replace macros in message: ", e);
            }
        }
        return returnString;
    }

    public static Map<String, String> getEnvVars(Run<?, ?> build, TaskListener listener) {
        Map<String, String> messageEnvVars = new HashMap<String, String>();
        if (build != null) {
            messageEnvVars.putAll(build.getCharacteristicEnvVars());
            try {
                messageEnvVars.putAll(build.getEnvironment(listener));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Couldn't get Env Variables: ", e);
            }
        }
        return messageEnvVars;
    }

    public String getNote(Run<?, ?> build, TaskListener listener) {
        String message_part;
        StringBuilder msg = new StringBuilder();
        String icon = getResultIcon(build.getResult());
        String buildUrl = Jenkins.getInstance().getRootUrl() + build.getUrl();
        String defaultNote = MessageFormat.format("{0} Jenkins Build {1}\n\nResults available at: [Jenkins [{2} #{3}]]({4})",
            icon, build.getResult().toString(), build.getParent().getDisplayName(), build.getNumber(), buildUrl);

        if (this.getReplaceSuccessNote() && build.getResult() == Result.SUCCESS) {
            message_part = replaceMacros(build, listener, this.getSuccessNoteText());
        } else if (this.getReplaceAbortNote() && build.getResult() == Result.ABORTED) {
            message_part = replaceMacros(build, listener, this.getAbortNoteText());
        } else if (this.getReplaceFailureNote() && build.getResult() == Result.FAILURE) {
            message_part = replaceMacros(build, listener, this.getFailureNoteText());
        } else {
            message_part = defaultNote;
        }
        msg.append(message_part);
        return msg.toString();
    }
}
