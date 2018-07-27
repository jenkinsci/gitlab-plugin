package com.dabsquared.gitlabjenkins.workflow;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.export.ExportedBean;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import com.google.common.collect.ImmutableSet;
import static com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty.getClient;

/**
 * @author <a href="mailto:deep.alexander@gmail.com">Alex Nordlund</a>
 */
@ExportedBean
public class AddGitLabMergeRequestEmojiStep extends Step {

    private static final Logger LOGGER = Logger.getLogger(AddGitLabMergeRequestEmojiStep.class.getName());

    private String emoji;

    @DataBoundConstructor
    public AddGitLabMergeRequestEmojiStep(String emoji) {
        this.emoji = StringUtils.isEmpty(emoji) ? null : emoji;
    }

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new AddGitLabMergeRequestEmojiStepExecution(context, this);
	}
	
    public String getEmoji() {
        return emoji;
    }

    @DataBoundSetter
    public void setEmoji(String emoji) {
        this.emoji = StringUtils.isBlank(emoji) ? null : emoji;
    }

    public static class AddGitLabMergeRequestEmojiStepExecution extends AbstractSynchronousStepExecution<Void> {
        private static final long serialVersionUID = 1;

        private final transient Run<?, ?> run;

        private final transient AddGitLabMergeRequestEmojiStep step;

        AddGitLabMergeRequestEmojiStepExecution(StepContext context, AddGitLabMergeRequestEmojiStep step) throws Exception {
            super(context);
            this.step = step;
            run = context.get(Run.class);
        }
        
        @Override
        protected Void run() {
            GitLabWebHookCause cause = run.getCause(GitLabWebHookCause.class);
            if (cause != null) {
                MergeRequest mergeRequest = cause.getData().getMergeRequest();
                if (mergeRequest != null) {
                    GitLabClient client = getClient(run);
                    if (client == null) {
                        println("No GitLab connection configured");
                    } else if (step.getEmoji() == null) {
                        error("No emoji given configured!");
                    } else {
                        try {
                            client.awardMergeRequestEmoji(mergeRequest, step.getEmoji());
                        } catch (NotFoundException e) {
                            String message = String.format("Failed to add vote on Merge Request for project '%s'\n" +
                                "Got unexpected 404, are you using the wrong API version, " +
                                "an unknown emoji, or trying to thumbs up/down your own merge request?", mergeRequest.getProjectId());
                            error(message);
                        } catch (WebApplicationException | ProcessingException e) {
                            printf("Failed to add emoji on Merge Request for project '%s': %s%n", mergeRequest.getProjectId(), e.getMessage());
                            LOGGER.log(Level.SEVERE, String.format("Failed to add emoji on Merge Request for project '%s'", mergeRequest.getProjectId()), e);
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

        private void error(String message) {
            TaskListener listener = getTaskListener();
            if (listener == null) {
                LOGGER.log(Level.FINE, "failed to print message {0} due to null TaskListener", message);
            } else {
                listener.error(message);
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
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public String getDisplayName() {
            return "Add emoji on GitLab Merge Request";
        }

        @Override
        public String getFunctionName() {
            return "addGitLabMREmoji";
        }
        
		@Override
		public Set<Class<?>> getRequiredContext() {
			return ImmutableSet.of(TaskListener.class, Run.class);
		}
    }
}
