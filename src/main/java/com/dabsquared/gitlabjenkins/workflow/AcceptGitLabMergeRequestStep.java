package com.dabsquared.gitlabjenkins.workflow;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.export.ExportedBean;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty.getClient;

/**
 * @author <a href="mailto:robin.mueller@1und1.de">Robin Müller</a>
 */
@ExportedBean
public class AcceptGitLabMergeRequestStep extends AbstractStepImpl {

    private static final Logger LOGGER = Logger.getLogger(AcceptGitLabMergeRequestStep.class.getName());

    private String mergeCommitMessage;

    @DataBoundConstructor
    public AcceptGitLabMergeRequestStep(String mergeCommitMessage) {
        this.mergeCommitMessage = StringUtils.isEmpty(mergeCommitMessage) ? null : mergeCommitMessage;
    }

    public String getMergeCommitMessage() {
        return mergeCommitMessage;
    }

    @DataBoundSetter
    public void setMergeCommitMessage(String mergeCommitMessage) {
        this.mergeCommitMessage = StringUtils.isEmpty(mergeCommitMessage) ? null : mergeCommitMessage;
    }

    public static class Execution extends AbstractSynchronousStepExecution<Void> {
        private static final long serialVersionUID = 1;

        @StepContextParameter
        private transient Run<?, ?> run;

        @Inject
        private transient AcceptGitLabMergeRequestStep step;

        @Override
        protected Void run() throws Exception {
            GitLabWebHookCause cause = run.getCause(GitLabWebHookCause.class);
            if (cause != null) {
                MergeRequest mergeRequest = cause.getData().getMergeRequest();
                if (mergeRequest != null) {
                    GitLabClient client = getClient(run);
                    if (client == null) {
                        println("No GitLab connection configured");
                    } else {
                        try {
                            client.acceptMergeRequest(mergeRequest, step.mergeCommitMessage, false);
                        } catch (WebApplicationException | ProcessingException e) {
                            printf("Failed to accept merge request for project '%s': %s%n", mergeRequest.getProjectId(), e.getMessage());
                            LOGGER.log(Level.SEVERE, String.format("Failed to accept merge request for project '%s'", mergeRequest.getProjectId()), e);
                        }
                    }
                }
            }
            return null;
        }

        private void println(String message) {
            TaskListener listener = getTaskListener();
            if (listener == null) {
                LOGGER.log(Level.FINE, "failed to print message {0} due to null TaskListener", message);
            } else {
                listener.getLogger().println(message);
            }
        }

        private void printf(String message, Object... args) {
            TaskListener listener = getTaskListener();
            if (listener == null) {
                LOGGER.log(Level.FINE, "failed to print message {0} due to null TaskListener", String.format(message, args));
            } else {
                listener.getLogger().printf(message, args);
            }
        }

        private TaskListener getTaskListener() {
            StepContext context = getContext();
            if (!context.isReady()) {
                return null;
            }
            try {
                return context.get(TaskListener.class);
            } catch (Exception x) {
                return null;
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getDisplayName() {
            return "Accept GitLab Merge Request";
        }

        @Override
        public String getFunctionName() {
            return "acceptGitLabMR";
        }
    }
}
