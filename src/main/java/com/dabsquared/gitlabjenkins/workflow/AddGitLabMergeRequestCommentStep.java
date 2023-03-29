package com.dabsquared.gitlabjenkins.workflow;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.export.ExportedBean;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty.getClient;

/**
 * @author <a href="mailto:robin.mueller@1und1.de">Robin Müller</a>
 */
@ExportedBean
public class AddGitLabMergeRequestCommentStep extends Step {

    private static final Logger LOGGER = Logger.getLogger(AddGitLabMergeRequestCommentStep.class.getName());

    private String comment;

    @DataBoundConstructor
    public AddGitLabMergeRequestCommentStep(String comment) {
        this.comment = StringUtils.isEmpty(comment) ? null : comment;
    }

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new AddGitLabMergeRequestCommentStepExecution(context, this);
	}
	
    public String getComment() {
        return comment;
    }

    @DataBoundSetter
    public void setComment(String comment) {
        this.comment = StringUtils.isEmpty(comment) ? null : comment;
    }

    public static class AddGitLabMergeRequestCommentStepExecution extends AbstractSynchronousStepExecution<Void> {
        private static final long serialVersionUID = 1;

        private final transient Run<?, ?> run;

        private final transient AddGitLabMergeRequestCommentStep step;

        AddGitLabMergeRequestCommentStepExecution(StepContext context, AddGitLabMergeRequestCommentStep step) throws Exception {
            super(context);
            this.step = step;
            run = context.get(Run.class);
        }
        
        @Override
        protected Void run() throws Exception {
            GitLabWebHookCause cause = run.getCause(GitLabWebHookCause.class);
            if (cause == null) {
                List<GitLabWebHookCause> gitLabWebHookCauses =
                    retrieveCauseRecursive(run.getCauses());
                if (!CollectionUtils.isEmpty(gitLabWebHookCauses)) {
                    cause = gitLabWebHookCauses.get(0);
                }
            }
            if (cause != null) {
                MergeRequest mergeRequest = cause.getData().getMergeRequest();
                if (mergeRequest != null) {
                    GitLabClient client = getClient(run);
                    if (client == null) {
                        println("No GitLab connection configured");
                    } else {
                        try {
                            client.createMergeRequestNote(mergeRequest, step.getComment());
                        } catch (WebApplicationException | ProcessingException e) {
                            printf("Failed to add comment on Merge Request for project '%s': %s%n", mergeRequest.getProjectId(), e.getMessage());
                            LOGGER.log(Level.SEVERE, String.format("Failed to add comment on Merge Request for project '%s'", mergeRequest.getProjectId()), e);
                        }
                    }
                }
            }
            else {
                LOGGER.log(Level.WARNING, "Add MR comment failure, " +
                  "Cannot retrieve GitLab MR context: Cannot find GitLabWebHookCause");
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
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public String getDisplayName() {
            return "Add comment on GitLab Merge Request";
        }

        @Override
        public String getFunctionName() {
            return "addGitLabMRComment";
        }
        
		@Override
		public Set<Class<?>> getRequiredContext() {
			Set<Class<?>> context = new HashSet<>();
			Collections.addAll(context, TaskListener.class, Run.class);
			return Collections.unmodifiableSet(context);
		}
    }


    /**
     * Retrieve cause recursively for nested job.
     * <p>
     * If child task invoked by parent, getCause(GitLabWebHookCause.class) will
     * return nothing due to task not triggered by Gitlab. So retrieve cause
     * from upstream task is needed.
     * <p>
     * Notice: Only retrieve the first founded GitLabWebHookCause instance.
     *
     * @param causes current level cause
     *
     * @return cause from parent
     *
     * @author Alceatraz Warprays
     */
    private static List<GitLabWebHookCause> retrieveCauseRecursive(List<Cause> causes) {
        for (Cause cause : causes) {
            if (!(cause instanceof UpstreamCause)) continue;
            List<Cause> upstreamCauses = ((UpstreamCause) cause)
                                             .getUpstreamCauses();
            for (Cause upCause : upstreamCauses) {
                if (!(upCause instanceof GitLabWebHookCause)) continue;
                return Collections.singletonList((GitLabWebHookCause) upCause);
            }
            List<GitLabWebHookCause> builds =
                retrieveCauseRecursive(upstreamCauses);
            if (!builds.isEmpty()) return builds;
        }
        return Collections.emptyList();
    }
}
