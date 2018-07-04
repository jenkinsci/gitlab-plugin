package com.dabsquared.gitlabjenkins.connection;


import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Run;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author Robin Müller
 */
public class GitLabConnectionProperty extends JobProperty<Job<?, ?>> {

    private String gitLabConnection;

    @DataBoundConstructor
    public GitLabConnectionProperty(String gitLabConnection) {
        this.gitLabConnection = gitLabConnection;
    }

    public String getGitLabConnection() {
        return gitLabConnection;
    }

    public GitLabClient getClient() {
        if (StringUtils.isNotEmpty(gitLabConnection)) {
            GitLabConnectionConfig connectionConfig = (GitLabConnectionConfig) Jenkins.getInstance().getDescriptor(GitLabConnectionConfig.class);
            return connectionConfig != null ? connectionConfig.getClient(gitLabConnection) : null;
        }
        return null;
    }

    public static GitLabClient getClient(Run<?, ?> build) {
        final GitLabConnectionProperty connectionProperty = build.getParent().getProperty(GitLabConnectionProperty.class);
        if (connectionProperty != null) {
            return connectionProperty.getClient();
        }
        return null;
    }

    @Extension
    @Symbol("gitLabConnection")
    public static class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return "GitLab connection";
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }

        @Override
        public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(GitLabConnectionProperty.class, formData);
        }

        public ListBoxModel doFillGitLabConnectionItems() {
            ListBoxModel options = new ListBoxModel();
            GitLabConnectionConfig descriptor = (GitLabConnectionConfig) Jenkins.getInstance().getDescriptor(GitLabConnectionConfig.class);
            for (GitLabConnection connection : descriptor.getConnections()) {
                options.add(connection.getName(), connection.getName());
            }
            return options;
        }
    }
}
