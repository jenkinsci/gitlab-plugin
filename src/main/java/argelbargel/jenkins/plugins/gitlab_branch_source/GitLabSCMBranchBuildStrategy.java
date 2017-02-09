package argelbargel.jenkins.plugins.gitlab_branch_source;


import hudson.Extension;
import jenkins.branch.BranchBuildStrategy;
import jenkins.branch.BranchBuildStrategyDescriptor;
import jenkins.branch.BranchSource;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceDescriptor;
import jenkins.scm.api.mixin.TagSCMHead;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;


public class GitLabSCMBranchBuildStrategy extends BranchBuildStrategy {
    static final GitLabSCMBranchBuildStrategy INSTANCE = new GitLabSCMBranchBuildStrategy();

    @Override
    public boolean isAutomaticBuild(SCMSource source, SCMHead head) {
        if (source instanceof GitLabSCMSource) {
            return isAutomaticBuild((GitLabSCMSource) source, head);
        }

        return !TagSCMHead.class.isInstance(head);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean isAutomaticBuild(GitLabSCMSource source, SCMHead head) {
        if (head instanceof TagSCMHead) {
            return source.getBuildTags();
        }

        if (head instanceof GitLabSCMBranchHead) {
            return !((GitLabSCMBranchHead) head).hasMergeRequest() || source.getBuildBranchesWithMergeRequests();
        }

        return true;
    }

    boolean isApplicable(BranchSource branchSource) {
        return getDescriptor().isApplicable(branchSource.getSource().getDescriptor());
    }

    @Extension
    public static class DescriptorImpl extends BranchBuildStrategyDescriptor {
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.GitLabSCMBranchBuildStrategy_DisplayName();
        }

        @Override
        public boolean isApplicable(@Nonnull SCMSourceDescriptor sourceDescriptor) {
            return sourceDescriptor instanceof GitLabSCMSource.DescriptorImpl;
        }

        @Override
        public BranchBuildStrategy newInstance(@CheckForNull StaplerRequest req, @Nonnull JSONObject formData) throws FormException {
            return INSTANCE;
        }
    }
}
