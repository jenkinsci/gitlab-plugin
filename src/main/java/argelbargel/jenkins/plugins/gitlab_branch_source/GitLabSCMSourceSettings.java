package argelbargel.jenkins.plugins.gitlab_branch_source;

import argelbargel.jenkins.plugins.gitlab_branch_source.api.filters.AllowMergeRequestsFromForks;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.filters.AllowMergeRequestsFromOrigin;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.filters.FilterWorkInProgress;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.filters.GitLabMergeRequestFilter;

import java.util.EnumSet;
import java.util.Set;

import static argelbargel.jenkins.plugins.gitlab_branch_source.DescriptorHelper.CHECKOUT_CREDENTIALS_ANONYMOUS;
import static java.util.Collections.emptySet;

class GitLabSCMSourceSettings {
    private static final String DEFAULT_INCLUDES = "*";
    private static final String DEFAULT_EXCLUDES = "";

    private final String connectionName;
    private final String credentialsId;
    private String includes;
    private String excludes;
    private boolean buildBranches;
    private boolean buildBranchesWithMergeRequests;
    private boolean buildTags;
    private Set<GitLabSCMMergeRequestBuildStrategy> originMergeRequestBuildStrategies;
    private Set<GitLabSCMMergeRequestBuildStrategy> forkMergeRequestBuildStrategies;
    private boolean ignoreOriginWIPMergeRequests;
    private boolean ignoreForkWIPMergeRequests;
    private boolean registerWebHooks;
    private boolean updateBuildDescription;


    GitLabSCMSourceSettings(String connectionName, String credentialsId) {
        this.connectionName = connectionName;
        this.credentialsId = credentialsId;
        this.includes = DEFAULT_INCLUDES;
        this.excludes = DEFAULT_EXCLUDES;
        this.buildBranches = true;
        this.buildBranchesWithMergeRequests = false;
        this.originMergeRequestBuildStrategies = EnumSet.allOf(GitLabSCMMergeRequestBuildStrategy.class);
        this.forkMergeRequestBuildStrategies = emptySet();
        this.buildTags = false;
        this.ignoreOriginWIPMergeRequests = true;
        this.ignoreForkWIPMergeRequests = true;
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
        return getBuildMergeRequestsFromOrigin() || getBuildMergeRequestsFromForks();
    }

    boolean getBuildMergeRequestsFromOrigin() {
        return !originMergeRequestBuildStrategies.isEmpty();
    }

    boolean getBuildMergeRequestsFromForks() {
        return !forkMergeRequestBuildStrategies.isEmpty();
    }

    Set<GitLabSCMMergeRequestBuildStrategy> getOriginMergeRequestBuildStrategies() {
        return originMergeRequestBuildStrategies;
    }

    void setOriginMergeRequestBuildStrategies(Set<GitLabSCMMergeRequestBuildStrategy> value) {
        originMergeRequestBuildStrategies = value;
    }

    Set<GitLabSCMMergeRequestBuildStrategy> getForkMergeRequestBuildStrategies() {
        return forkMergeRequestBuildStrategies;
    }

    void setForkMergeRequestBuildStrategies(Set<GitLabSCMMergeRequestBuildStrategy> value) {
        forkMergeRequestBuildStrategies = value;
    }

    boolean getIgnoreOriginWIPMergeRequests() {
        return ignoreOriginWIPMergeRequests;
    }

    void setIgnoreOriginWIPMergeRequests(boolean value) {
        ignoreOriginWIPMergeRequests = value;
    }

    boolean getIgnoreForkWIPMergeRequests() {
        return ignoreForkWIPMergeRequests;
    }

    void setIgnoreForkWIPMergeRequests(boolean value) {
        ignoreForkWIPMergeRequests = value;
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
        if (getBuildMergeRequestsFromOrigin()) {
            GitLabMergeRequestFilter originFilter = new AllowMergeRequestsFromOrigin();
            if (getIgnoreOriginWIPMergeRequests()) {
                originFilter = originFilter.and(new FilterWorkInProgress());
            }

            filter = filter.or(originFilter);
        }

        if (getBuildMergeRequestsFromForks()) {
            GitLabMergeRequestFilter forkFilter = new AllowMergeRequestsFromForks();
            if (getIgnoreOriginWIPMergeRequests()) {
                forkFilter = forkFilter.and(new FilterWorkInProgress());
            }

            filter = filter.or(forkFilter);
        }

        return filter;
    }
}
