package argelbargel.jenkins.plugins.gitlab_branch_source.api.filters;

import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabMergeRequest;

import java.util.Objects;

public class AllowMergeRequestsFromForks extends GitLabMergeRequestFilter {
    @Override
    public boolean accepts(GitLabMergeRequest mr) {
        return !Objects.equals(mr.getSourceProjectId(), mr.getTargetProjectId());
    }
}
