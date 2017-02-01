package argelbargel.jenkins.plugins.gitlab_branch_source.api.filters;

import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabMergeRequest;

public class FilterWorkInProgress extends GitLabMergeRequestFilter {
    @Override
    public boolean accepts(GitLabMergeRequest mr) {
        return !mr.isWorkInProgress();
    }
}
