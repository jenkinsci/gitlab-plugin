package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadCategory;
import jenkins.scm.impl.ChangeRequestSCMHeadCategory;
import jenkins.scm.impl.TagSCMHeadCategory;
import jenkins.scm.impl.UncategorizedSCMHeadCategory;
import org.jvnet.localizer.Localizable;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

class GitLabSCMHeadCategory extends SCMHeadCategory {
    private static final SCMHeadCategory UNCATEGORIZED = new UncategorizedSCMHeadCategory(Messages._GitLabSCMHeadCategory_Uncategorized());
    private static final SCMHeadCategory BRANCH_WITH_MERGE_REQUEST = new GitLabSCMHeadCategory(new BranchWithMergeRequestCategory(Messages._GitLabSCMHeadCategory_BranchesWithMergeRequest()));
    private static final SCMHeadCategory MERGE_REQUEST = new GitLabSCMHeadCategory(new ChangeRequestSCMHeadCategory(Messages._GitLabSCMHeadCategory_MergeRequests()));
    private static final SCMHeadCategory TAG = new GitLabSCMHeadCategory(new TagSCMHeadCategory(Messages._GitLabSCMHeadCategory_Tags()));
    static final SCMHeadCategory[] ALL = new SCMHeadCategory[] { UNCATEGORIZED, BRANCH_WITH_MERGE_REQUEST, MERGE_REQUEST, TAG };


    private final SCMHeadCategory delegate;

    private GitLabSCMHeadCategory(SCMHeadCategory delegate) {
        super(delegate.getName(), delegate.getDisplayName());
        this.delegate = delegate;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean isMatch(@Nonnull SCMHead head) {
        return delegate.isMatch(head);
    }

    private static class BranchWithMergeRequestCategory extends SCMHeadCategory {
        BranchWithMergeRequestCategory(@CheckForNull Localizable pronoun) {
            super("branchesWithMergeRequests", pronoun);
        }

        @Override
        public boolean isMatch(@Nonnull SCMHead head) {
            return head instanceof GitLabSCMBranchHead && ((GitLabSCMBranchHead) head).hasMergeRequest();
        }
    }
}
