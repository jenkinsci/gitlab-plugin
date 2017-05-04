package com.dabsquared.gitlabjenkins.workflow;

import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.BodyExecution;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.export.ExportedBean;

import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.util.CommitStatusUpdater;
import com.google.common.collect.ImmutableSet;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * @author <a href="mailto:robin.mueller@1und1.de">Robin Müller</a>
 */
@ExportedBean
public class GitLabCommitStatusStep extends Step {

    private String name;

    @DataBoundConstructor
    public GitLabCommitStatusStep(String name) {
        this.name = StringUtils.isEmpty(name) ? null : name;
    }
    
	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new GitLabCommitStatusStepExecution(context, this);
	}

    public String getName() {
        return name;
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = StringUtils.isEmpty(name) ? null : name;
    }

    public static class GitLabCommitStatusStepExecution extends StepExecution {
        private static final long serialVersionUID = 1;

        private final transient Run<?, ?> run;

        private final transient GitLabCommitStatusStep step;

        private BodyExecution body;

        GitLabCommitStatusStepExecution(StepContext context, GitLabCommitStatusStep step) throws Exception {
            super(context);
            this.step = step;
            run = context.get(Run.class);
        }
        
        @Override
        public boolean start() throws Exception {
            final String name = StringUtils.isEmpty(step.name) ? "jenkins" : step.name;
            body = getContext().newBodyInvoker()
                .withCallback(new BodyExecutionCallback() {
                    @Override
                    public void onStart(StepContext context) {
                        CommitStatusUpdater.updateCommitStatus(run, getTaskListener(context), BuildState.running, name);
                        PendingBuildsAction action = run.getAction(PendingBuildsAction.class);
                        if (action != null) {
                            action.startBuild(name);
                        }
                    }

                    @Override
                    public void onSuccess(StepContext context, Object result) {
                        CommitStatusUpdater.updateCommitStatus(run, getTaskListener(context), BuildState.success, name);
                        context.onSuccess(result);
                    }

                    @Override
                    public void onFailure(StepContext context, Throwable t) {
                        BuildState state = t instanceof FlowInterruptedException ? BuildState.canceled : BuildState.failed;
                        CommitStatusUpdater.updateCommitStatus(run, getTaskListener(context), state, name);
                        context.onFailure(t);
                    }
                })
                .start();
            return false;
        }

        @Override
        public void stop(@Nonnull Throwable cause) throws Exception {
            // should be no need to do anything special (but verify in JENKINS-26148)
            if (body != null) {
                String name = StringUtils.isEmpty(step.name) ? "jenkins" : step.name;
                CommitStatusUpdater.updateCommitStatus(run, null, BuildState.canceled, name);
                body.cancel(cause);
            }
        }

        private TaskListener getTaskListener(StepContext context) {
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
            return "Update the commit status in GitLab depending on the build status";
        }

        @Override
        public String getFunctionName() {
            return "gitlabCommitStatus";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

		@Override
		public Set<Class<?>> getRequiredContext() {
			return ImmutableSet.of(TaskListener.class, Run.class);
		}
    }
}
