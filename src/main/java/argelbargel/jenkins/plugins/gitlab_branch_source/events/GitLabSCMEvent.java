package argelbargel.jenkins.plugins.gitlab_branch_source.events;


import hudson.model.Cause;

public interface GitLabSCMEvent {
    Cause getCause();
}
