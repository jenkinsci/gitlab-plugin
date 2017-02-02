package argelbargel.jenkins.plugins.gitlab_branch_source;

import argelbargel.jenkins.plugins.gitlab_branch_source.api.filters.AllowMergeRequestsFromForks;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.filters.AllowMergeRequestsFromOrigin;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.filters.FilterWorkInProgress;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.filters.GitLabMergeRequestFilter;

import static argelbargel.jenkins.plugins.gitlab_branch_source.DescriptorHelper.CHECKOUT_CREDENTIALS_ANONYMOUS;

class SourceSettings {
    private static final String DEFAULT_INCLUDES = "*";
    private static final String DEFAULT_EXCLUDES = "";

    private final String connectionName;
    private final String credentialsId;
    private String includes;
    private String excludes;
    private boolean buildBranches;
    private boolean buildBranchesWithMergeRequests;
    private boolean buildTags;
    private MergeRequestBuildStrategy originMergeRequestBuildStrategy;
    private MergeRequestBuildStrategy forkMergeRequestBuildStrategy;
    private boolean registerWebHooks;
    private boolean updateBuildDescription;


    SourceSettings(String connectionName, String credentialsId) {
        this.connectionName = connectionName;
        this.credentialsId = credentialsId;
        this.includes = DEFAULT_INCLUDES;
        this.excludes = DEFAULT_EXCLUDES;
        this.buildBranches = true;
        this.buildBranchesWithMergeRequests = false;
        this.originMergeRequestBuildStrategy = new MergeRequestBuildStrategy(true);
        this.forkMergeRequestBuildStrategy = new MergeRequestBuildStrategy(false);
        this.buildTags = false;
        this.registerWebHooks = true;
        this.updateBuildDescription = true;
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

    boolean getBuildBranches() {
        return buildBranches;
    }

    void setBuildBranches(boolean value) {
        buildBranches = value;
    }

    boolean getBuildTags() {
        return buildTags;
    }

    void setBuildTags(boolean buildTags) {
        this.buildTags = buildTags;
    }

    boolean getBuildMergeRequests() {
        return originMergeRequestBuildStrategy.enabled() || forkMergeRequestBuildStrategy.enabled();
    }

    MergeRequestBuildStrategy originMergeRequestBuildStrategy() {
        return originMergeRequestBuildStrategy;
    }

    MergeRequestBuildStrategy forkMergeRequestBuildStrategy() {
        return forkMergeRequestBuildStrategy;
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

    GitLabMergeRequestFilter getMergeRequestFilter() {
        GitLabMergeRequestFilter filter = GitLabMergeRequestFilter.ALLOW_NONE;
        if (originMergeRequestBuildStrategy.enabled()) {
            GitLabMergeRequestFilter originFilter = new AllowMergeRequestsFromOrigin();
            if (originMergeRequestBuildStrategy.ignoreWorkInProgress()) {
                originFilter = originFilter.and(new FilterWorkInProgress());
            }

            filter = filter.or(originFilter);
        }

        if (forkMergeRequestBuildStrategy.enabled()) {
            GitLabMergeRequestFilter forkFilter = new AllowMergeRequestsFromForks();
            if (forkMergeRequestBuildStrategy.ignoreWorkInProgress()) {
                forkFilter = forkFilter.and(new FilterWorkInProgress());
            }

            filter = filter.or(forkFilter);
        }

        return filter;
    }
}
