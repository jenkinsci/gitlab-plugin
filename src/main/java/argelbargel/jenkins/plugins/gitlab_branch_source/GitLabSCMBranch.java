package argelbargel.jenkins.plugins.gitlab_branch_source;

import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabBranchCommit;

import javax.annotation.CheckForNull;

class GitLabSCMBranch extends GitLabSCMHead {
    GitLabSCMBranch(GitlabBranch branch) {
        this(branch.getName(), branch.getCommit());
    }

    GitLabSCMBranch(String name, String hash) {
        super(name, hash);
    }

    private GitLabSCMBranch(String name, GitlabBranchCommit commit) {
        this(name, commit.getId());
    }

    @CheckForNull
    public String getPronoun() {
        return Messages.GitLabSCMBranch_Pronoun();
    }
}
