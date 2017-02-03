package argelbargel.jenkins.plugins.gitlab_branch_source;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

class GitLabSCMHeadImpl extends GitLabSCMHead {
    private final String hash;
    private final String pronoun;

    GitLabSCMHeadImpl(@Nonnull String pronoun, @Nonnull String name, String hash) {
        super(name);
        this.pronoun = pronoun;
        this.hash = hash;
    }

    @Override
    public final GitLabSCMCommit getCommit() {
        return new GitLabSCMCommit(this, hash);
    }

    @Override
    @CheckForNull
    public final String getPronoun() {
        return pronoun;
    }
}
