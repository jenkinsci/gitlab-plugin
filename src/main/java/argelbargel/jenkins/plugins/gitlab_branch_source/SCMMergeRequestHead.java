package argelbargel.jenkins.plugins.gitlab_branch_source;

import javax.annotation.Nonnull;

class SCMMergeRequestHead extends SCMHeadImpl {
    private final GitLabSCMHead source;
    private final GitLabSCMHead target;

    SCMMergeRequestHead(@Nonnull String name, GitLabSCMHead source, GitLabSCMHead target) {
        super(Messages.GitLabSCMMergeRequest_Pronoun(), name, "HEAD");
        this.source = source;
        this.target = target;
    }

    GitLabSCMHead getSource() {
        return source;
    }

    GitLabSCMHead getTarget() {
        return target;
    }
}
