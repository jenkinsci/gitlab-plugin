package argelbargel.jenkins.plugins.gitlab_branch_source;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

abstract class GitLabSCMHeadImpl extends GitLabSCMHead {
    private final String pronoun;
    private final String refBase;

    GitLabSCMHeadImpl(@Nonnull String name, @Nonnull String pronoun, @Nonnull String refBase) {
        super(name);
        this.pronoun = pronoun;
        this.refBase = refBase;
    }

    @Override
    @CheckForNull
    public final String getPronoun() {
        return pronoun;
    }

    @Nonnull
    @Override
    final String getRef() {
        return refBase + getName();
    }
}
