package com.dabsquared.gitlabjenkins.workflow;

import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.util.CommitStatusUpdater;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.BodyExecution;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.ExportedBean;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * @author <a href="mailto:robin.mueller@1und1.de">Robin Müller</a>
 */
@ExportedBean
public class GitLabCommitStatusStep extends AbstractStepImpl {

    private String name;

    @DataBoundConstructor
    public GitLabCommitStatusStep(String name) {
        this.name = StringUtils.isEmpty(name) ? null : name;
    }

    public String getName() {
        return name;
    }

    public static class Execution extends AbstractStepExecutionImpl {
        private static final long serialVersionUID = 1;

        @StepContextParameter
        private transient Run<?, ?> run;

        @Inject
        private transient GitLabCommitStatusStep step;

        private BodyExecution body;

        @Override
        public boolean start() throws Exception {
            final String name = StringUtils.isEmpty(step.name) ? "jenkins" : step.name;
            body = getContext().newBodyInvoker()
                .withCallback(new BodyExecutionCallback() {
                    @Override
                    public void onStart(StepContext context) {
                        CommitStatusUpdater.updateCommitStatus(run, getTaskListener(context), BuildState.running, name);
                    }

                    @Override
                    public void onSuccess(StepContext context, Object result) {
                        CommitStatusUpdater.updateCommitStatus(run, getTaskListener(context), BuildState.success, name);
                        context.onSuccess(result);
                    }

                    @Override
                    public void onFailure(StepContext context, Throwable t) {
                        CommitStatusUpdater.updateCommitStatus(run, getTaskListener(context), BuildState.failed, name);
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
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() {
            super(Execution.class);
        }

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
    }
}
