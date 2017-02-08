package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.scm.api.mixin.TagSCMHead;

import javax.annotation.Nonnull;

public final class GitLabSCMTagHead extends GitLabSCMHeadImpl implements TagSCMHead {
    private final String hash;
    private final long timestamp;

    GitLabSCMTagHead(@Nonnull String name, String hash, long timestamp) {
        super(name, Messages.GitLabSCMTag_Pronoun(), ORIGIN_REF_TAGS);
        this.hash = hash;
        this.timestamp = timestamp;
    }

    @Nonnull
    @Override
    public SCMRevisionImpl getRevision() {
        return new SCMRevisionImpl(this, hash);
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
