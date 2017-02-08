package argelbargel.jenkins.plugins.gitlab_branch_source;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import hudson.model.Cause;
import hudson.model.CauseAction;

class GitLabSCMCauseAction extends CauseAction {
    private final boolean updateBuildDescription;

    GitLabSCMCauseAction(Cause c, boolean updateBuildDescription) {
        super(c);
        this.updateBuildDescription = updateBuildDescription;
    }

    String getDescription() {
        return findCause(GitLabWebHookCause.class).getShortDescription();
    }

    boolean updateBuildDescription() {
        return updateBuildDescription;
    }
}
