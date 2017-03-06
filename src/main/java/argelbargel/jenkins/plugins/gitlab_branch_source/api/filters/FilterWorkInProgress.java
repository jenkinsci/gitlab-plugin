package argelbargel.jenkins.plugins.gitlab_branch_source.api.filters;

import argelbargel.jenkins.plugins.gitlab_branch_source.Messages;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabMergeRequest;
import hudson.model.TaskListener;


public class FilterWorkInProgress extends GitLabMergeRequestFilter {
    public FilterWorkInProgress(TaskListener listener) {
        super(listener);
    }

    @Override
    public boolean accepts(GitLabMergeRequest mr) {
        return !mr.isWorkInProgress();
    }

    @Override
    protected String reason() {
        return Messages.GitLabMergeRequestFilter_filterWorkInProgress();
    }
}
