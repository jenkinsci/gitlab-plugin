package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.mixin.SCMHeadMixin;

public abstract class GitLabSCMHead extends SCMHead implements SCMHeadMixin {
    GitLabSCMHead(String name) {
        super(name);
    }

    public abstract SCMRevision getRevision();
}
