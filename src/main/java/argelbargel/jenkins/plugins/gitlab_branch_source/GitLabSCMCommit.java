package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMHead;
import org.gitlab.api.models.GitlabBranchCommit;

import javax.annotation.Nonnull;

public class GitLabSCMCommit extends AbstractGitSCMSource.SCMRevisionImpl {
    public GitLabSCMCommit(@Nonnull SCMHead head, String hash) {
        super(head, hash);
    }

    GitLabSCMCommit(@Nonnull SCMHead head, @Nonnull GitlabBranchCommit commit) {
        this(head, commit.getId());
    }
}
