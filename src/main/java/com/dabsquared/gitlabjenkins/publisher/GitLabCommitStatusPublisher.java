package com.dabsquared.gitlabjenkins.publisher;

import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.util.CommitStatusUpdater;
import hudson.Extension;
import hudson.Launcher;
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
public class GitLabCommitStatusPublisher extends Notifier {

    private String name;

    @DataBoundConstructor
    public GitLabCommitStatusPublisher(String name) {
        this.name = name;
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
        if (buildResult == Result.SUCCESS) {
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

    protected GitLabCommitStatusPublisher readResolve() {
        if (name == null) {
            name = "jenkins";
        }
        return this;
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
