package argelbargel.jenkins.plugins.gitlab_branch_source;

import javax.annotation.Nonnull;

class GitLabSCMMergeRequest extends GitLabSCMHeadImpl {
    private final GitLabSCMHead source;
    private final GitLabSCMHead target;

    GitLabSCMMergeRequest(@Nonnull String name, GitLabSCMHead source, GitLabSCMHead target) {
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
