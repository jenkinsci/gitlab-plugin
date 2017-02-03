package argelbargel.jenkins.plugins.gitlab_branch_source.api.filters;

import argelbargel.jenkins.plugins.gitlab_branch_source.Messages;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabMergeRequest;
import hudson.model.TaskListener;

import java.util.Objects;

public class AllowMergeRequestsFromOrigin extends GitLabMergeRequestFilter {
    public AllowMergeRequestsFromOrigin(TaskListener listener) {
        super(listener);
    }

    @Override
    public boolean accepts(GitLabMergeRequest mr) {
        return Objects.equals(mr.getTargetProjectId(), mr.getSourceProjectId());
    }


    @Override
    protected String reason() {
        return Messages.GitLabMergeRequestFilter_allowMergeRequestsFromOrigin();
    }

}
