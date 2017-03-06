package argelbargel.jenkins.plugins.gitlab_branch_source.api.filters;

import argelbargel.jenkins.plugins.gitlab_branch_source.Messages;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabMergeRequest;
import hudson.model.TaskListener;

import java.util.Objects;

public class AllowMergeRequestsFromForks extends GitLabMergeRequestFilter {
    public AllowMergeRequestsFromForks(TaskListener listener) {
        super(listener);
    }

    @Override
    public boolean accepts(GitLabMergeRequest mr) {
        return !Objects.equals(mr.getSourceProjectId(), mr.getTargetProjectId());
    }

    @Override
    protected String reason() {
        return Messages.GitLabMergeRequestFilter_allowMergeRequestsFromForks();
    }
}
