package com.dabsquared.gitlabjenkins.publisher;

import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.util.CommitStatusUpdater;
import hudson.Extension;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

/**
 * @author Robin Müller
 */
public class GitLabCommitStatusPublisher extends Notifier implements MatrixAggregatable {

    private String name;
    private boolean markUnstableAsSuccess;

    @DataBoundConstructor
    public GitLabCommitStatusPublisher(String name, boolean markUnstableAsSuccess) {
        this.name = name;
        this.markUnstableAsSuccess = markUnstableAsSuccess;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        CommitStatusUpdater.updateCommitStatus(build, listener, BuildState.running, name);
        return true;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        Result buildResult = build.getResult();
        if (buildResult == Result.SUCCESS || (buildResult == Result.UNSTABLE && markUnstableAsSuccess)) {
            CommitStatusUpdater.updateCommitStatus(build, listener, BuildState.success, name);
        } else if (buildResult == Result.ABORTED) {
            CommitStatusUpdater.updateCommitStatus(build, listener, BuildState.canceled, name);
        } else {
            CommitStatusUpdater.updateCommitStatus(build, listener, BuildState.failed, name);
        }
        return true;
    }

    public String getName() {
        return name;
    }

    public boolean isMarkUnstableAsSuccess() {
        return markUnstableAsSuccess;
    }

    protected GitLabCommitStatusPublisher readResolve() {
        if (name == null) {
            name = "jenkins";
        }
        return this;
    }

    public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
        return new MatrixAggregator(build, launcher, listener) {
            @Override
            public boolean endBuild() throws InterruptedException, IOException {
                perform(build, launcher, listener);
                return super.endBuild();
            }
        };
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.GitLabCommitStatusPublisher_DisplayName();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/gitlab-plugin/help/help-gitlab8.1CI.html";
        }

        public FormValidation doCheckName(@QueryParameter String value) {
            if (StringUtils.isEmpty(value)) {
                return FormValidation.error(Messages.name_required());
            }
            return FormValidation.ok();
        }
    }
}
