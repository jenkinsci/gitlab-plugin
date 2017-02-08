package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;

import javax.annotation.Nonnull;

final class GitLabSCMBranchHead extends GitLabSCMHeadImpl {
    private final boolean hasMergeRequest;
    private final String hash;

    GitLabSCMBranchHead(@Nonnull String name, String hash, boolean hasMergeRequest) {
        super(name, Messages.GitLabSCMBranch_Pronoun(), ORIGIN_REF_BRANCHES);
        this.hash = hash;
        this.hasMergeRequest = hasMergeRequest;
    }

    @Nonnull
    @Override
    public SCMRevisionImpl getRevision() {
        return new SCMRevisionImpl(this, hash);
    }

    boolean hasMergeRequest() {
        return hasMergeRequest;
    }
}
