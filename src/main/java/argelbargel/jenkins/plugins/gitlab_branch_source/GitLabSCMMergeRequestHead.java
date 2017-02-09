package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.scm.api.mixin.ChangeRequestSCMHead;

import javax.annotation.Nonnull;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMRefSpec.MERGE_REQUESTS;

public final class GitLabSCMMergeRequestHead extends GitLabSCMHeadImpl implements ChangeRequestSCMHead {
    private final int id;
    private final String title;
    private final GitLabSCMHead source;
    private final GitLabSCMHead target;
    private final boolean merge;

    GitLabSCMMergeRequestHead(int id, String title, GitLabSCMHead source, GitLabSCMHead target, boolean merge) {
        super(title + " (!" + id + ")" + (merge ? " merged" : ""), Messages.GitLabSCMMergeRequest_Pronoun(), MERGE_REQUESTS);
        this.id = id;
        this.title = title;
        this.source = source;
        this.target = target;
        this.merge = merge;
    }

    @Nonnull
    @Override
    public String getId() {
        return String.valueOf(id);
    }

    @Nonnull
    @Override
    public GitLabSCMHead getTarget() {
        return target;
    }

    @Nonnull
    @Override
    public SCMRevisionImpl getRevision() {
        return source.getRevision();
    }

    public GitLabSCMMergeRequestHead merged() {
        return new GitLabSCMMergeRequestHead(id, title, source, target, true);
    }
}
