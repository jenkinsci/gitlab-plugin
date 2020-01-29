package com.dabsquared.gitlabjenkins.workflow;

import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.util.CommitStatusUpdater;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;

import java.io.Serial;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.export.ExportedBean;

import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:robin.mueller@1und1.de">Robin Müller</a>
 */
@ExportedBean
public class UpdateGitLabCommitStatusStep extends Step {

    private String name;
    private BuildState state;
    private List<GitLabBranchBuild> builds = new ArrayList<GitLabBranchBuild>() ;
    private GitLabConnectionProperty connection;

    @DataBoundConstructor
    public UpdateGitLabCommitStatusStep(String name, BuildState state) {
        this.name = StringUtils.isEmpty(name) ? null : name;
        this.state = state;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new UpdateGitLabCommitStatusStepExecution(context, this);
    }

    public String getName() {
        return name;
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = StringUtils.isEmpty(name) ? null : name;
    }

    public BuildState getState() {
        return state;
    }

    @DataBoundSetter
    public void setState(BuildState state) {
        this.state = state;
    }

    public List<GitLabBranchBuild> getBuilds() {
        return builds;
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

    public static class UpdateGitLabCommitStatusStepExecution extends AbstractSynchronousStepExecution<Void> {
        @Serial
        private static final long serialVersionUID = 1;

        private final transient Run<?, ?> run;

        private final transient UpdateGitLabCommitStatusStep step;

        UpdateGitLabCommitStatusStepExecution(StepContext context, UpdateGitLabCommitStatusStep step) throws Exception {
            super(context);
            this.step = step;
            run = context.get(Run.class);
        }

        @Override
        protected Void run() throws Exception {
            final String name = StringUtils.isEmpty(step.name) ? "jenkins" : step.name;
            CommitStatusUpdater.updateCommitStatus(run, getTaskListener(), step.state, name);
            PendingBuildsAction action = run.getAction(PendingBuildsAction.class);
            if (action != null) {
                action.startBuild(name);
            }
            return null;
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
        public @NonNull String getDisplayName() {
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

        @Override
        public Set<Class<?>> getRequiredContext() {
            Set<Class<?>> context = new HashSet<>();
            Collections.addAll(context, TaskListener.class, Run.class);
            return Collections.unmodifiableSet(context);
        }
    }
}
