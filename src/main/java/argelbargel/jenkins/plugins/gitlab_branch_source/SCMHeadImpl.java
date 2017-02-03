package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMRevision;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

class SCMHeadImpl extends GitLabSCMHead {
    private final String hash;
    private final String pronoun;

    SCMHeadImpl(@Nonnull String pronoun, @Nonnull String name, String hash) {
        super(name);
        this.pronoun = pronoun;
        this.hash = hash;
    }

    @Override
    public final SCMRevision getRevision() {
        return new AbstractGitSCMSource.SCMRevisionImpl(this, hash);
    }

    @Override
    @CheckForNull
    public final String getPronoun() {
        return pronoun;
    }
}
