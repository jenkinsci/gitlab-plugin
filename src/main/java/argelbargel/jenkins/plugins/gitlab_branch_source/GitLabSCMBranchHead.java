package argelbargel.jenkins.plugins.gitlab_branch_source;

import javax.annotation.Nonnull;

final class GitLabSCMBranchHead extends GitLabSCMHeadImpl {
    private final boolean hasMergeRequest;

    GitLabSCMBranchHead(@Nonnull String name, String hash, boolean hasMergeRequest) {
        super(name, hash, Messages.GitLabSCMBranch_Pronoun(), ORIGIN_REF_BRANCHES);
        this.hasMergeRequest = hasMergeRequest;
    }

    boolean hasMergeRequest() {
        return hasMergeRequest;
    }
}
