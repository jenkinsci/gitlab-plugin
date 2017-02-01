package argelbargel.jenkins.plugins.gitlab_branch_source;

import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabMergeRequest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Objects;

class GitLabSCMMergeRequest extends GitLabSCMHead {
    private final GitLabMergeRequest delegate;

    GitLabSCMMergeRequest(@Nonnull GitLabMergeRequest delegate) {
        super(String.valueOf(delegate.getId()), "HEAD");
        this.delegate = delegate;
    }

    boolean isFromOrigin() {
        return Objects.equals(delegate.getSourceProjectId(), delegate.getTargetProjectId());
    }

    boolean isFromFork() {
        return !isFromOrigin();
    }

    boolean isWorkInProgress() {
        return delegate.isWorkInProgress();
    }

    GitLabSCMBranch getSource() {
        return new GitLabSCMBranch(delegate.getSourceBranch(), delegate.getSha());
    }

    GitLabSCMBranch getTarget() {
        return new GitLabSCMBranch(delegate.getTargetBranch(), "HEAD");
    }

    @CheckForNull
    public String getPronoun() {
        return Messages.GitLabSCMMergeRequest_Pronoun();
    }
}
