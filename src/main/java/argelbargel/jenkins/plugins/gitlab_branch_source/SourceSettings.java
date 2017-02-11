package argelbargel.jenkins.plugins.gitlab_branch_source;


import argelbargel.jenkins.plugins.gitlab_branch_source.api.filters.AllowMergeRequestsFromForks;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.filters.AllowMergeRequestsFromOrigin;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.filters.FilterWorkInProgress;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.filters.GitLabMergeRequestFilter;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

import static argelbargel.jenkins.plugins.gitlab_branch_source.BuildStatusPublishMode.NONE;
import static argelbargel.jenkins.plugins.gitlab_branch_source.BuildStatusPublishMode.STAGES;
import static argelbargel.jenkins.plugins.gitlab_branch_source.DescriptorHelper.CHECKOUT_CREDENTIALS_ANONYMOUS;


class SourceSettings {
    private static final String DEFAULT_INCLUDES = "*";
    private static final String DEFAULT_EXCLUDES = "";
    private static final String DEFAULT_MERGE_COMMIT_MESSAGE = "Accepted Merge-Request #{0} after build {1} succeeded";

    private final String connectionName;
    private final String credentialsId;
    private String includes;
    private String excludes;
    private final MonitorStrategy branchMonitorStrategy;
    private final MonitorStrategy originMonitorStrategy;
    private final MonitorStrategy forksMonitorStrategy;
    private final MonitorStrategy tagMonitorStrategy;
    private boolean buildBranchesWithMergeRequests;
    private boolean registerWebHooks;
    private boolean updateBuildDescription;
    private String publisherName;
    private boolean publishUnstableBuildsAsSuccess;
    private String mergeCommitMessage;


    SourceSettings(String connectionName, String credentialsId) {
        this.connectionName = connectionName;
        this.credentialsId = credentialsId;
        this.includes = DEFAULT_INCLUDES;
        this.excludes = DEFAULT_EXCLUDES;
        this.branchMonitorStrategy = new MonitorStrategy(true, false, STAGES);
        this.buildBranchesWithMergeRequests = false;
        this.originMonitorStrategy = new MonitorStrategy(true, true, STAGES);
        this.forksMonitorStrategy = new MonitorStrategy(false, true, STAGES);
        this.tagMonitorStrategy = new MonitorStrategy(false, false, NONE);
        this.registerWebHooks = true;
        this.updateBuildDescription = true;
        this.publisherName = Jenkins.getInstance().getDisplayName();
        this.publishUnstableBuildsAsSuccess = false;
        this.mergeCommitMessage = DEFAULT_MERGE_COMMIT_MESSAGE;
    }

    String getConnectionName() {
        return connectionName;
    }

    String getCredentialsId() {
        return !CHECKOUT_CREDENTIALS_ANONYMOUS.equals(credentialsId) ? credentialsId : null;
    }

    String getIncludes() {
        return includes;
    }

    void setIncludes(String includes) {
        this.includes = (includes != null) ? includes : DEFAULT_INCLUDES;
    }

    String getExcludes() {
        return excludes;
    }

    void setExcludes(String excludes) {
        this.excludes = (excludes != null) ? excludes : DEFAULT_EXCLUDES;
    }

    boolean getBuildBranchesWithMergeRequests() {
        return buildBranchesWithMergeRequests;
    }

    void setBuildBranchesWithMergeRequests(boolean value) {
        buildBranchesWithMergeRequests = value;
    }

    MonitorStrategy branchMonitorStrategy() {
        return branchMonitorStrategy;
    }

    MonitorStrategy tagMonitorStrategy() {
        return tagMonitorStrategy;
    }

    MonitorStrategy originMonitorStrategy() {
        return originMonitorStrategy;
    }

    MonitorStrategy forksMonitorStrategy() {
        return forksMonitorStrategy;
    }

    boolean getRegisterWebHooks() {
        return registerWebHooks;
    }

    void setRegisterWebHooks(boolean value) {
        registerWebHooks = value;
    }

    void setUpdateBuildDescription(boolean value) {
        updateBuildDescription = value;
    }

    boolean getUpdateBuildDescription() {
        return updateBuildDescription;
    }

    void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    String getPublisherName() {
        return publisherName;
    }

    void setPublishUnstableBuildsAsSuccess(boolean publishUnstableBuildsAsSuccess) {
        this.publishUnstableBuildsAsSuccess = publishUnstableBuildsAsSuccess;
    }

    boolean getPublishUnstableBuildsAsSuccess() {
        return publishUnstableBuildsAsSuccess;
    }

    String getMergeCommitMessage() {
        return mergeCommitMessage;
    }

    void setMergeCommitMessage(String value) {
        this.mergeCommitMessage = value;
    }

    GitLabMergeRequestFilter createMergeRequestFilter(TaskListener listener) {
        GitLabMergeRequestFilter filter = GitLabMergeRequestFilter.ALLOW_NONE;
        if (originMonitorStrategy.monitored()) {
            GitLabMergeRequestFilter originFilter = new AllowMergeRequestsFromOrigin(listener);
            if (originMonitorStrategy.ignoreWorkInProgress()) {
                originFilter = originFilter.and(new FilterWorkInProgress(listener));
            }

            filter = filter.or(originFilter);
        }

        if (forksMonitorStrategy.monitored()) {
            GitLabMergeRequestFilter forkFilter = new AllowMergeRequestsFromForks(listener);
            if (forksMonitorStrategy.ignoreWorkInProgress()) {
                forkFilter = forkFilter.and(new FilterWorkInProgress(listener));
            }

            filter = filter.or(forkFilter);
        }

        return filter;
    }

    boolean determineMergeRequestStrategyValue(GitLabSCMMergeRequestHead head, boolean originStrategy, boolean forksStrategy) {
        boolean fromOrigin = head.fromOrigin();
        return (fromOrigin && originStrategy) || (!fromOrigin && forksStrategy);
    }
}
