package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.scm.api.SCMHead;

class GitLabSCMHolder extends SCMHead {
    private final SCMHead target;

    GitLabSCMHolder(SCMHead head) {
        this(head, head);
    }

    GitLabSCMHolder(SCMHead head, SCMHead target) {
        super(head.getPronoun() + " " + head.getName());
        this.target = target;
    }

    SCMHead getTarget() {
        return target;
    }
}
