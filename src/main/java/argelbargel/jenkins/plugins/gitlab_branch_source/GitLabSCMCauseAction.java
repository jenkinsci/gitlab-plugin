package argelbargel.jenkins.plugins.gitlab_branch_source;

import hudson.model.Cause;
import hudson.model.CauseAction;

class GitLabSCMCauseAction extends CauseAction {
    private final boolean updateBuildDescription;

    GitLabSCMCauseAction(Cause c, boolean updateBuildDescription) {
        super(c);
        this.updateBuildDescription = updateBuildDescription;
    }

    boolean updateBuildDescription() {
        return updateBuildDescription;
    }
}
