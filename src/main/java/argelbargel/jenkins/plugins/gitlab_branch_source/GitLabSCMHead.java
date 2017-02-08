package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.mixin.SCMHeadMixin;

import javax.annotation.Nonnull;

public abstract class GitLabSCMHead extends SCMHead implements SCMHeadMixin {
    public static final String REVISION_HEAD = "HEAD";

    static final String ORIGIN_REF_BRANCHES = "refs/heads/";
    static final String ORIGIN_REF_TAGS = "refs/tags/";
    static final String ORIGIN_REF_MERGE_REQUESTS = "refs/merge-requests/";


    public static GitLabSCMHead createBranch(String name, String hash) {
        return createBranch(name, hash, false);
    }

    public static GitLabSCMHead createTag(String name, String hash, long timestamp) {
        return new GitLabSCMTagHead(name, hash, timestamp);
    }

    public static GitLabSCMMergeRequestHead createMergeRequest(int id, String name, GitLabSCMHead source, GitLabSCMHead target) {
        return new GitLabSCMMergeRequestHead(id, name, source, target, false);
    }

    static GitLabSCMHead createBranch(String name, String hash, boolean hasMergeRequest) {
        return new GitLabSCMBranchHead(name, hash, hasMergeRequest);
    }


    GitLabSCMHead(String name) {
        super(name);
    }

    @Nonnull
    public abstract SCMRevisionImpl getRevision();

    @Nonnull
    abstract String getRef();
}
