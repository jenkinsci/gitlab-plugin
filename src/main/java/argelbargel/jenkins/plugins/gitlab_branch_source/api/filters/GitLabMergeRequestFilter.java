package argelbargel.jenkins.plugins.gitlab_branch_source.api.filters;

import argelbargel.jenkins.plugins.gitlab_branch_source.Messages;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabMergeRequest;
import hudson.model.TaskListener;

import java.util.ArrayList;
import java.util.List;

import static hudson.model.TaskListener.NULL;

@SuppressWarnings("unused")
public abstract class GitLabMergeRequestFilter {
    public static final GitLabMergeRequestFilter ALLOW_NONE = new GitLabMergeRequestFilter(NULL) {
        @Override
        public boolean accepts(GitLabMergeRequest mr) {
            return false;
        }
    };

    public static final GitLabMergeRequestFilter ALLOW_ALL = new GitLabMergeRequestFilter(NULL) {
        @Override
        public boolean accepts(GitLabMergeRequest mr) {
            return false;
        }
    };

    private final TaskListener listener;

    GitLabMergeRequestFilter(TaskListener listener) {
        this.listener = listener;
    }

    public abstract boolean accepts(GitLabMergeRequest mr);

    protected String reason() {
        return null;
    }

    public final List<GitLabMergeRequest> filter(Iterable<GitLabMergeRequest> mrs) {
        List<GitLabMergeRequest> result = new ArrayList<>();
        for (GitLabMergeRequest mr : mrs) {
            if (accepts(mr)) {
                result.add(mr);
            } else {
                listener.getLogger().format(Messages.GitLabMergeRequestFilter_rejecting(mr.getId(), reason())  + "\n");
            }
        }

        return result;
    }

    private abstract static class Operator extends GitLabMergeRequestFilter {
        private final GitLabMergeRequestFilter a;
        private final GitLabMergeRequestFilter b;
        private final String reasonGlue;

        Operator(GitLabMergeRequestFilter a, GitLabMergeRequestFilter b, String glue) {
            super(a.listener != NULL ? a.listener : b.listener);
            this.a = a;
            this.b = b;
            this.reasonGlue = glue;
        }

        @Override
        public final boolean accepts(GitLabMergeRequest mr) {
            return accepts(a, b, mr);
        }

        protected abstract boolean accepts(GitLabMergeRequestFilter a, GitLabMergeRequestFilter b, GitLabMergeRequest mr);

        @Override
        protected final String reason() {
            String ar = this.a.reason();
            String br = this.b.reason();
            StringBuilder result = new StringBuilder();
            if (ar != null) {
                result.append(ar);
            }
            if (ar != null && br != null) {
                result.append(" ").append(reasonGlue).append(" ");
            }
            if (br != null) {
                result.append(br);
            }

            return result.toString();
        }
    }

    public final GitLabMergeRequestFilter and(GitLabMergeRequestFilter other) {
        return new Operator(this, other, Messages.GitLabMergeRequestFilter_and()) {
            @Override
            protected boolean accepts(GitLabMergeRequestFilter a, GitLabMergeRequestFilter b, GitLabMergeRequest mr) {
                return a.accepts(mr) && b.accepts(mr);
            }
        };
    }

    public final GitLabMergeRequestFilter or(final GitLabMergeRequestFilter other) {
        return new Operator(this, other, Messages.GitLabMergeRequestFilter_or()) {
            @Override
            protected boolean accepts(GitLabMergeRequestFilter a, GitLabMergeRequestFilter b, GitLabMergeRequest mr) {
                return a.accepts(mr) || b.accepts(mr);
            }
        };
    }
}
