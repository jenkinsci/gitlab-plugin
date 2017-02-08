package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.scm.api.mixin.ChangeRequestSCMHead;

import javax.annotation.Nonnull;

final class GitLabSCMMergeRequestHead extends GitLabSCMHeadImpl implements ChangeRequestSCMHead {
    private final String id;
    private final GitLabSCMHead source;
    private final GitLabSCMHead target;

    GitLabSCMMergeRequestHead(int id, String name, GitLabSCMHead source, GitLabSCMHead target) {
        super(name + " (!" + id + ")", Messages.GitLabSCMMergeRequest_Pronoun(), ORIGIN_REF_MERGE_REQUESTS);
        this.id = String.valueOf(id);
        this.source = source;
        this.target = target;
    }

    @Nonnull
    @Override
    public String getId() {
        return id;
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
}
