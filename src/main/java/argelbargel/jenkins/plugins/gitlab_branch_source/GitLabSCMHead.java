package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.scm.api.SCMHead;
import jenkins.scm.api.mixin.SCMHeadMixin;

public class GitLabSCMHead extends SCMHead implements SCMHeadMixin {
    private final String hash;

    @SuppressWarnings("WeakerAccess")
    protected GitLabSCMHead(String name, String hash) {
        super(name);
        this.hash = hash;
    }

    public final GitLabSCMCommit getCommit() {
        return new GitLabSCMCommit(this, hash);
    }
}
