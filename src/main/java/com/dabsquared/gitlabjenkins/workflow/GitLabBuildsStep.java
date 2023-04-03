package com.dabsquared.gitlabjenkins.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.NonNull;

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

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * @author <a href="mailto:robin.mueller@1und1.de">Robin Müller</a>
 */
@ExportedBean
public class GitLabBuildsStep extends Step {

    private List<String> builds;

    @DataBoundConstructor
    public GitLabBuildsStep() {
    }

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new GitLabBuildStepExecution(context, this);
	}

    @DataBoundSetter
    public void setBuilds(List<String> builds) {
        if (builds != null && builds.size() == 1) {
            this.builds = new ArrayList<>();
            for (String build : builds.get(0).split(",")) {
                if (!build.isEmpty()) {
                    this.builds.add(build.trim());
                }
            }
        } else {
            this.builds = builds;
        }
    }

    public List<String> getBuilds() {
        return builds;
    }

    public static class GitLabBuildStepExecution extends StepExecution {
        private static final long serialVersionUID = 1;

        private final transient Run<?, ?> run;

        private final transient GitLabBuildsStep step;

        private BodyExecution body;

        GitLabBuildStepExecution(StepContext context, GitLabBuildsStep step) throws Exception {
            super(context);
            this.step = step;
            run = context.get(Run.class);
        }
        
        @Override
        public boolean start() throws Exception {
            body = getContext().newBodyInvoker()
                .withCallback(new BodyExecutionCallback() {
                    @Override
                    public void onStart(StepContext context) {
                        for (String name : step.builds) {
                            CommitStatusUpdater.updateCommitStatus(run, getTaskListener(context), BuildState.pending, name);
                        }
                        run.addAction(new PendingBuildsAction(new ArrayList<>(step.builds)));
                    }

                    @Override
                    public void onSuccess(StepContext context, Object result) {
                        PendingBuildsAction action = run.getAction(PendingBuildsAction.class);
                        if (action != null && !action.getBuilds().isEmpty()) {
                            TaskListener taskListener = getTaskListener(context);
                            if (taskListener != null) {
                                taskListener.getLogger().println("There are still pending GitLab builds. Please check your configuration");
                            }
                        }
                        context.onSuccess(result);
                    }

                    @Override
                    public void onFailure(StepContext context, Throwable t) {
                        PendingBuildsAction action = run.getAction(PendingBuildsAction.class);
                        if (action != null) {
                            BuildState state = t instanceof FlowInterruptedException ? BuildState.canceled : BuildState.failed;
                            for (String name : action.getBuilds()) {
                                CommitStatusUpdater.updateCommitStatus(run, getTaskListener(context), state, name);
                            }
                        }
                        context.onFailure(t);
                    }
                })
                .start();
            return false;
        }

        @Override
        public void stop(@NonNull Throwable cause) throws Exception {
            // should be no need to do anything special (but verify in JENKINS-26148)
            if (body != null) {
                PendingBuildsAction action = run.getAction(PendingBuildsAction.class);
                if (action != null) {
                    for (String name : action.getBuilds()) {
                        CommitStatusUpdater.updateCommitStatus(run, null, BuildState.canceled, name);
                    }
                }
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
            return "Notify gitlab about pending builds";
        }

        @Override
        public String getFunctionName() {
            return "gitlabBuilds";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

		@Override
		public Set<Class<?>> getRequiredContext() {
			Set<Class<?>> context = new HashSet<>();
			Collections.addAll(context, TaskListener.class, Run.class);
			return Collections.unmodifiableSet(context);
		}
    }
}
