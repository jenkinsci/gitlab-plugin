package com.dabsquared.gitlabjenkins.workflow;

import static com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty.getClient;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.MergeRequest;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * @author <a href="mailto:robin.mueller@1und1.de">Robin Müller</a>
 */
@ExportedBean
public class AcceptGitLabMergeRequestStep extends Step {

    private static final Logger LOGGER = Logger.getLogger(AcceptGitLabMergeRequestStep.class.getName());

    private String mergeCommitMessage;

    private boolean useMRDescription;

    private Boolean removeSourceBranch;

    @Deprecated
    public AcceptGitLabMergeRequestStep(
            String mergeCommitMessage, boolean useMRDescription, boolean removeSourceBranch) {
        this.mergeCommitMessage = StringUtils.isEmpty(mergeCommitMessage) ? null : mergeCommitMessage;
        this.useMRDescription = useMRDescription;
        this.removeSourceBranch = removeSourceBranch;
    }

    @DataBoundConstructor
    public AcceptGitLabMergeRequestStep() {}

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new AcceptGitLabMergeRequestStepExecution(context, this);
    }

    public String getMergeCommitMessage() {
        return mergeCommitMessage;
    }

    public Boolean getRemoveSourceBranch() {
        return this.removeSourceBranch;
    }

    public boolean getUseMRDescription() {
        return this.useMRDescription;
    }

    @DataBoundSetter
    public void setMergeCommitMessage(String mergeCommitMessage) {
        this.mergeCommitMessage = StringUtils.isEmpty(mergeCommitMessage) ? null : mergeCommitMessage;
    }

    @DataBoundSetter
    public void setUseMRDescription(boolean useMRDescription) {
        this.useMRDescription = useMRDescription;
    }

    @DataBoundSetter
    public void setRemoveSourceBranch(boolean removeSourceBranch) {
        this.removeSourceBranch = removeSourceBranch;
    }

    public static class AcceptGitLabMergeRequestStepExecution extends AbstractSynchronousStepExecution<Void> {
        private static final long serialVersionUID = 1;

        private final transient Run<?, ?> run;

        private final transient AcceptGitLabMergeRequestStep step;

        AcceptGitLabMergeRequestStepExecution(StepContext context, AcceptGitLabMergeRequestStep step) throws Exception {
            super(context);
            this.step = step;
            run = context.get(Run.class);
        }

        @Override
        protected Void run() throws Exception {
            GitLabWebHookCause cause = run.getCause(GitLabWebHookCause.class);
            if (cause != null) {
                MergeRequest mergeRequest = getMergeRequest(run);
                if (mergeRequest != null) {
                    GitLabApi client = getClient(run);
                    if (client == null) {
                        println("No GitLab connection configured");
                    } else {
                        try {
                            client.getMergeRequestApi()
                                    .acceptMergeRequest(
                                            mergeRequest.getProjectId(),
                                            mergeRequest.getIid(),
                                            getCommitMessage(mergeRequest),
                                            true,
                                            true);
                        } catch (GitLabApiException e) {
                            printf(
                                    "Failed to accept merge request for project '%s': %s%n",
                                    mergeRequest.getProjectId(), e.getMessage());
                            LOGGER.log(
                                    Level.SEVERE,
                                    String.format(
                                            "Failed to accept merge request for project '%s'",
                                            mergeRequest.getProjectId()),
                                    e);
                        }
                    }
                }
            }
            return null;
        }

        private MergeRequest getMergeRequest(Run<?, ?> run) throws GitLabApiException {
            GitLabWebHookCause cause = run.getCause(GitLabWebHookCause.class);
            return cause == null ? null : cause.getData().getMergeRequest(run);
        }

        private String getCommitMessage(MergeRequest mergeRequest) {
            if (!step.useMRDescription) return step.mergeCommitMessage;

            String message = "Merge branch '" + mergeRequest.getSourceBranch() + "' into '"
                    + mergeRequest.getTargetBranch() + "'\n\n" + mergeRequest.getTitle()
                    + "\n\n" + mergeRequest.getDescription()
                    + "\n\n" + "See merge request !"
                    + mergeRequest.getIid();
            return message;
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
                LOGGER.log(
                        Level.FINE,
                        "failed to print message {0} due to null TaskListener",
                        String.format(message, args));
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
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public String getDisplayName() {
            return "Accept GitLab Merge Request";
        }

        @Override
        public String getFunctionName() {
            return "acceptGitLabMR";
        }

        @Override
        public Set<Class<?>> getRequiredContext() {
            Set<Class<?>> context = new HashSet<>();
            Collections.addAll(context, TaskListener.class, Run.class);
            return Collections.unmodifiableSet(context);
        }
    }
}
