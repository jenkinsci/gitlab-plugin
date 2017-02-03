package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.mixin.TagSCMHead;

/**
 * Changes the name/label of the job for the given head
 */
abstract class SCMHeadLabel extends GitLabSCMHead {
    static SCMHeadLabel create(GitLabSCMHead head, boolean automaticBuild) {
        String label = head.getPronoun() + " " + head.getName();
        return (automaticBuild) ? new AutomaticBuildLabel(label, head) : new ManualBuildLabel(label, head);
    }


    private final GitLabSCMHead target;

    SCMHeadLabel(String label, GitLabSCMHead target) {
        super(label);
        this.target = target;
    }

    @Override
    public final SCMRevision getRevision() {
        return target.getRevision();
    }

    final GitLabSCMHead getTarget() {
        return target;
    }


    private static class AutomaticBuildLabel extends SCMHeadLabel {
        AutomaticBuildLabel(String label, GitLabSCMHead target) {
            super(label, target);
        }
    }

    // HACK ALERT: the current default buildstrategy of MultiBranchProject does not build TagSCMHead instances automatically;
    // when our sources are observed by another Project which uses different buildstrategies this might not work...
    private static class ManualBuildLabel extends SCMHeadLabel implements TagSCMHead {
        ManualBuildLabel(String label, GitLabSCMHead target) {
            super(label, target);
        }

        @Override
        public long getTimestamp() {
            return 0;
        }
    }
}
