package com.dabsquared.gitlabjenkins.workflow;

import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitLabBranchBuild extends AbstractDescribableImpl<GitLabBranchBuild> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitLabBranchBuild.class);

    private String name;
    private String projectId;
    private String revisionHash;
    private GitLabConnectionProperty connection;

    @DataBoundConstructor
    public GitLabBranchBuild() {}

    public GitLabBranchBuild(String projectId, String revisionHash) {
        this.name = null;
        this.projectId = projectId;
        this.revisionHash = revisionHash;
        this.connection = null;
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = StringUtils.isEmpty(name) ? null : name;
    }

    @DataBoundSetter
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @DataBoundSetter
    public void setRevisionHash(String revisionHash) {
        this.revisionHash = revisionHash;
    }

    @DataBoundSetter
    public void setConnection(GitLabConnectionProperty connection) {
        this.connection = connection;
    }

    public void setConnection(String connection) {
        this.connection = StringUtils.isEmpty(connection) ? null : new GitLabConnectionProperty(connection);
    }

    public String getName() {
        return name;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getRevisionHash() {
        return revisionHash;
    }

    public GitLabConnectionProperty getConnection() {
        return connection;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<GitLabBranchBuild> {
        @NonNull
        @Override
        public String getDisplayName() {
            return "GitLab Branch Build";
        }
    }
}
