package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.mixin.SCMHeadMixin;

import javax.annotation.Nonnull;

public abstract class GitLabSCMHead extends SCMHead implements SCMHeadMixin {
    static final String ORIGIN_REF_BRANCHES = "refs/heads/";
    static final String ORIGIN_REF_TAGS = "refs/tags/";
    static final String ORIGIN_REF_MERGE_REQUESTS = "refs/merge-requests/";


    static GitLabSCMHead createBranch(String name) {
        return createBranch(name, "HEAD");
    }

    static GitLabSCMHead createBranch(String name, String hash) {
        return createBranch(name, hash, false);
    }

    static GitLabSCMHead createBranch(String name, String hash, boolean hasMergeRequest) {
        return new GitLabSCMBranchHead(name, hash, hasMergeRequest);
    }

    static GitLabSCMHead createTag(String name, String hash, long timestamp) {
        return new GitLabSCMTagHead(name, hash, timestamp);
    }

    static GitLabSCMMergeRequestHead createMergeRequest(int id, String name, GitLabSCMHead source, GitLabSCMHead target) {
        return new GitLabSCMMergeRequestHead(id, name, source, target, false);
    }

    GitLabSCMHead(String name) {
        super(name);
    }

    @Nonnull
    public abstract SCMRevisionImpl getRevision();

    @Nonnull
    abstract String getRef();
}
