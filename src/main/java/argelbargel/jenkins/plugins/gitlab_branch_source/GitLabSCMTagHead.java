package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.scm.api.mixin.TagSCMHead;

import javax.annotation.Nonnull;

final class GitLabSCMTagHead extends GitLabSCMHeadImpl implements TagSCMHead {
    private final long timestamp;

    GitLabSCMTagHead(@Nonnull String name, String hash, long timestamp) {
        super(name, hash, Messages.GitLabSCMTag_Pronoun(), ORIGIN_REF_TAGS);
        this.timestamp = timestamp;
    }

    @Override
    public final long getTimestamp() {
        return timestamp;
    }
}
