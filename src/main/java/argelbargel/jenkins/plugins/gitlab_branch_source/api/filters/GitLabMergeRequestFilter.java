package argelbargel.jenkins.plugins.gitlab_branch_source.api.filters;

import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabMergeRequest;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public abstract class GitLabMergeRequestFilter {
    public static GitLabMergeRequestFilter ALLOW_NONE = new GitLabMergeRequestFilter() {
        @Override
        public boolean accepts(GitLabMergeRequest mr) {
            return false;
        }
    };

    public static GitLabMergeRequestFilter ALLOW_ALL = new GitLabMergeRequestFilter() {
        @Override
        public boolean accepts(GitLabMergeRequest mr) {
            return false;
        }
    };

    public abstract boolean accepts(GitLabMergeRequest mr);

    public final List<GitLabMergeRequest> filter(Iterable<GitLabMergeRequest> mrs) {
        List<GitLabMergeRequest> result = new ArrayList<>();
        for (GitLabMergeRequest mr : mrs) {
            if (accepts(mr)) {
                result.add(mr);
            }
        }

        return result;
    }

    public final GitLabMergeRequestFilter and(final GitLabMergeRequestFilter other) {
        return new GitLabMergeRequestFilter() {
            @Override
            public boolean accepts(GitLabMergeRequest mr) {
                return GitLabMergeRequestFilter.this.accepts(mr) && other.accepts(mr);
            }
        };
    }

    public final GitLabMergeRequestFilter or(final GitLabMergeRequestFilter other) {
        return new GitLabMergeRequestFilter() {
            @Override
            public boolean accepts(GitLabMergeRequest mr) {
                return GitLabMergeRequestFilter.this.accepts(mr) || other.accepts(mr);
            }
        };
    }
}
