package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

class GitLabSCMHeadImpl extends GitLabSCMHead {
    private final String pronoun;
    private final String refBase;
    private final String hash;

    GitLabSCMHeadImpl(@Nonnull String name, String hash, @Nonnull String pronoun, @Nonnull String refBase) {
        super(name);
        this.pronoun = pronoun;
        this.refBase = refBase;
        this.hash = hash;
    }

    @Override
    @CheckForNull
    public final String getPronoun() {
        return pronoun;
    }

    @Override
    public final SCMRevisionImpl getRevision() {
        return new SCMRevisionImpl(this, hash);
    }

    @Override
    final String getRef() {
        return refBase + getName();
    }
}
