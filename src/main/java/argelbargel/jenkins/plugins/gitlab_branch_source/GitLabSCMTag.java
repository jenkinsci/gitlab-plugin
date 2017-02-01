package argelbargel.jenkins.plugins.gitlab_branch_source;

import org.gitlab.api.models.GitlabTag;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

class GitLabSCMTag extends GitLabSCMHead {
    GitLabSCMTag(@Nonnull GitlabTag tag) {
        super(tag.getName(), tag.getCommit().getId());
    }

    @CheckForNull
    public String getPronoun() {
        return Messages.GitLabSCMTag_Pronoun();
    }
}
