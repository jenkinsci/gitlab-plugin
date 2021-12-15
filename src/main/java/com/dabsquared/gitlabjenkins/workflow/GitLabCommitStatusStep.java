package com.dabsquared.gitlabjenkins.workflow;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.util.CommitStatusUpdater;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
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

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:robin.mueller@1und1.de">Robin Müller</a>
 */
@ExportedBean
public class GitLabCommitStatusStep extends Step {

    private String name;
    private List<GitLabBranchBuild> builds = new ArrayList<GitLabBranchBuild>() ;
    private GitLabConnectionProperty connection;
    private String state;

    @DataBoundConstructor
    public GitLabCommitStatusStep(String name){
        this.name = StringUtils.isEmpty(name) ? null : name;
    }

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new GitLabCommitStatusStepExecution(context, this);
	}

    public String getName() {
        return name;
    }

    public List<GitLabBranchBuild> getBuilds() {
        return builds;
    }

    @DataBoundSetter
    public void setState(String state){
        this.state = StringUtils.isEmpty(state) ? null : state;
    }

    public String getState() {
        return state;
    }

    @DataBoundSetter
    public void setBuilds(List<GitLabBranchBuild> builds) {
        this.builds = builds;
    }

    public GitLabConnectionProperty getConnection() {
        return connection;
    }

    @DataBoundSetter
    public void setConnection(GitLabConnectionProperty connection) {
        this.connection = connection;
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
                        if (step.state == null) {
                            CommitStatusUpdater.updateCommitStatus(run, getTaskListener(context), BuildState.running, name, step.builds, step.connection);
                            PendingBuildsAction action = run.getAction(PendingBuildsAction.class);
                            if (action != null) {
                                action.startBuild(name);
                            }
                        } else if (step.state == "pending") {
                            CommitStatusUpdater.updateCommitStatus(run, getTaskListener(context), BuildState.pending, name, step.builds, step.connection);
                        }
                    }

                    @Override
                    public void onSuccess(StepContext context, Object result) {
                        if (step == null || step.state == null || step.state != "pending") {
                            CommitStatusUpdater.updateCommitStatus(run, getTaskListener(context), BuildState.success, name,  step.builds, step.connection);
                        }
                        context.onSuccess(result);
                    }

                    @Override
                    public void onFailure(StepContext context, Throwable t) {
                        BuildState state = BuildState.failed;
                        if (t instanceof FlowInterruptedException) {
                            FlowInterruptedException ex = (FlowInterruptedException) t;
                            if (ex.isActualInterruption()) {
                                state = BuildState.canceled;
                            }
                        }

                        CommitStatusUpdater.updateCommitStatus(run, getTaskListener(context), state, name,  step.builds, step.connection);
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
                CommitStatusUpdater.updateCommitStatus(run, null, BuildState.canceled, name,  step.builds, step.connection);
                body.cancel(cause);
            }
        }

        private TaskListener getTaskListener(StepContext context) {
            try {
                if (!context.isReady()) {
                    return null;
                }
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
			Set<Class<?>> context = new HashSet<>();
			Collections.addAll(context, TaskListener.class, Run.class);
			return Collections.unmodifiableSet(context);
		}
    }
}
