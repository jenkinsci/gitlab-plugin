package com.dabsquared.gitlabjenkins.workflow;

import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.util.CommitStatusUpdater;
import hudson.Extension;
import hudson.model.Run;
import hudson.util.ListBoxModel;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.ExportedBean;

import javax.inject.Inject;
import java.util.EnumSet;

/**
 * @author <a href="mailto:robin.mueller@1und1.de">Robin Müller</a>
 */
@ExportedBean
public class UpdateGitLabCommitStatusStep extends AbstractStepImpl {

    private String name;
    private BuildState state;

    @DataBoundConstructor
    public UpdateGitLabCommitStatusStep(String name, BuildState state) {
        this.name = StringUtils.isEmpty(name) ? null : name;
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public BuildState getState() {
        return state;
    }

    public static class Execution extends AbstractSynchronousStepExecution<Void> {
        private static final long serialVersionUID = 1;

        @StepContextParameter
        private transient Run<?, ?> run;

        @Inject
        private transient UpdateGitLabCommitStatusStep step;

        @Override
        protected Void run() throws Exception {
            final String name = StringUtils.isEmpty(step.name) ? "jenkins" : step.name;
            CommitStatusUpdater.updateCommitStatus(run, null, step.state, name);
            PendingBuildsAction action = run.getAction(PendingBuildsAction.class);
            if (action != null) {
                action.startBuild(name);
            }
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getDisplayName() {
            return "Update the commit status in GitLab";
        }

        @Override
        public String getFunctionName() {
            return "updateGitlabCommitStatus";
        }

        public ListBoxModel doFillStateItems() {
            ListBoxModel options = new ListBoxModel();
            for (BuildState buildState : EnumSet.allOf(BuildState.class)) {
                options.add(buildState.name());
            }
            return options;
        }
    }
}
