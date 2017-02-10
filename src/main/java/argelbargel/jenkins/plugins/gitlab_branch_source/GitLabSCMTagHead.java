package argelbargel.jenkins.plugins.gitlab_branch_source;


import jenkins.scm.api.mixin.TagSCMHead;

import javax.annotation.Nonnull;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMRefSpec.TAGS;

public final class GitLabSCMTagHead extends GitLabSCMHeadImpl implements TagSCMHead {
    private final long timestamp;

    GitLabSCMTagHead(int projectId, @Nonnull String name, String hash, long timestamp) {
        super(projectId, name, hash, Messages.GitLabSCMTag_Pronoun(), TAGS);
        this.timestamp = timestamp;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
