package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.scm.api.mixin.TagSCMHead;

/**
 * Changes the name/label of the job for the given head
 */
abstract class HeadLabel extends GitLabSCMHead {
    static HeadLabel createLabel(GitLabSCMHead head) {
        return createLabel(head, (!(head instanceof GitLabSCMBranchHead) || !((GitLabSCMBranchHead) head).hasMergeRequest()));
    }


    static HeadLabel createLabel(GitLabSCMHead head, boolean automaticBuild) {
        return (automaticBuild) ? new AutomaticBuildLabel(head.getName(), head) : new ManualBuildLabel(head.getName(), head);
    }

    private final GitLabSCMHead target;

    private HeadLabel(String label, GitLabSCMHead target) {
        super(label);
        this.target = target;
    }

    @Override
    public final SCMRevisionImpl getRevision() {
        return target.getRevision();
    }

    @Override
    final String getRef() {
        return getHead().getRef();
    }

    final GitLabSCMHead getHead() {
        return target;
    }

    private static class AutomaticBuildLabel extends HeadLabel {
        AutomaticBuildLabel(String label, GitLabSCMHead target) {
            super(label, target);
        }
    }

    // HACK ALERT: the current default buildstrategy of MultiBranchProject does not build TagSCMHead instances automatically;
    // when our sources are observed by another Project which uses different buildstrategies this might not work...
    private static class ManualBuildLabel extends HeadLabel implements TagSCMHead {
        ManualBuildLabel(String label, GitLabSCMHead target) {
            super(label, target);
        }

        @Override
        public long getTimestamp() {
            return 0;
        }
    }
}
