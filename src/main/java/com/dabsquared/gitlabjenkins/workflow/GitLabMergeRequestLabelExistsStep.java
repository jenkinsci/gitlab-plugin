package com.dabsquared.gitlabjenkins.workflow;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class GitLabMergeRequestLabelExistsStep extends Step {

    private String label;

    @DataBoundConstructor
    public GitLabMergeRequestLabelExistsStep(String label) {
        this.label = StringUtils.isEmpty(label) ? null : label;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new GitLabMergeRequestLabelExistsStepExecution(context, this);
    }

    public String getLabel() {
        return label;
    }

    @DataBoundSetter
    public void setLabel(String label) {
        this.label = StringUtils.isEmpty(label) ? null : label;
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "GitLabMergeRequestLabelExists";
        }

        @Override
        public String getDisplayName() {
            return "Check if a GitLab merge request has a specific label";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            Set<Class<?>> context = new HashSet<>();
            Collections.addAll(context, TaskListener.class, Run.class);
            return Collections.unmodifiableSet(context);
        }
    }

    public static class GitLabMergeRequestLabelExistsStepExecution extends AbstractSynchronousStepExecution<Boolean> {
        private static final long serialVersionUID = 1;

        private final transient Run<?, ?> run;

        private final transient GitLabMergeRequestLabelExistsStep step;

        public GitLabMergeRequestLabelExistsStepExecution(StepContext context, GitLabMergeRequestLabelExistsStep step)
                throws Exception {
            super(context);
            this.step = step;
            run = context.get(Run.class);
        }

        @Override
        protected Boolean run() throws Exception {
            GitLabWebHookCause cause = run.getCause(GitLabWebHookCause.class);
            if (cause == null) {
                return false;
            }
            List<String> labels = cause.getData().getMergeRequestLabels();
            if (labels == null) {
                return false;
            }
            return labels.contains(step.getLabel());
        }
    }
}
