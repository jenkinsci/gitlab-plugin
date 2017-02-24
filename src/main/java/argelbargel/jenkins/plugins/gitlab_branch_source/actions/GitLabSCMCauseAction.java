package argelbargel.jenkins.plugins.gitlab_branch_source.actions;


import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import hudson.model.Cause;
import hudson.model.CauseAction;
import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;


public final class GitLabSCMCauseAction extends CauseAction {
    private final GitLabSCMHead head;
    private final String hash;

    public GitLabSCMCauseAction(SCMRevisionImpl revision, Cause... causes) {
        super(causes);
        this.head = (GitLabSCMHead) revision.getHead();
        this.hash = revision.getHash();
    }

    public String getDescription() {
        GitLabWebHookCause cause = findCause(GitLabWebHookCause.class);
        return (cause != null) ? cause.getShortDescription() : null;
    }

    public String getRef() {
        return head.getRef();
    }

    String getHash() {
        return hash;
    }
}
