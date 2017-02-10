package argelbargel.jenkins.plugins.gitlab_branch_source;


import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import hudson.model.Cause;
import hudson.model.CauseAction;
import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;


class GitLabSCMCauseAction extends CauseAction {
    private final GitLabSCMHead head;
    private final String hash;

    GitLabSCMCauseAction(SCMRevisionImpl revision, Cause... causes) {
        super(causes);
        this.head = (GitLabSCMHead) revision.getHead();
        this.hash = revision.getHash();
    }

    String getDescription() {
        GitLabWebHookCause cause = findCause(GitLabWebHookCause.class);
        return (cause != null) ? cause.getShortDescription() : null;
    }

    int getProjectId() {
        return head.getProjectId();
    }

    String getRef() {
        return head.getRef();
    }

    String getHash() {
        return hash;
    }
}
