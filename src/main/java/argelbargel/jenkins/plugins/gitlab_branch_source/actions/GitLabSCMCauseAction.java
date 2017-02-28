package argelbargel.jenkins.plugins.gitlab_branch_source.actions;


import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import hudson.model.Cause;
import hudson.model.CauseAction;


public final class GitLabSCMCauseAction extends CauseAction {
    public GitLabSCMCauseAction(Cause... causes) {
        super(causes);
    }

    public String getDescription() {
        GitLabWebHookCause cause = findCause(GitLabWebHookCause.class);
        return (cause != null) ? cause.getShortDescription() : null;
    }

}
