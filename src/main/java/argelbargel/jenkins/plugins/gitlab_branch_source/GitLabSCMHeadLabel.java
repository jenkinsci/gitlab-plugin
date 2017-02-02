package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.scm.api.SCMHead;
import jenkins.scm.api.mixin.TagSCMHead;

/**
 * Changes the name/label of the job for the given head
 */
abstract class GitLabSCMHeadLabel extends SCMHead {
    static GitLabSCMHeadLabel create(SCMHead head) {
        return create(head, true);
    }

    static GitLabSCMHeadLabel create(SCMHead head, boolean automaticBuild) {
        return create(head, head, automaticBuild);
    }

    static GitLabSCMHeadLabel create(SCMHead head, SCMHead target) {
        return create(head, target, true);
    }

    private static GitLabSCMHeadLabel create(SCMHead head, SCMHead target, boolean automaticBuild) {
        return create(head.getPronoun() + " " + head.getName(), target, automaticBuild);
    }

    private static GitLabSCMHeadLabel create(String label, SCMHead target, boolean automaticBuild) {
        return (automaticBuild) ? new AutomaticBuildLabel(label, target) : new ManualBuildLabel(label, target);
    }


    private final SCMHead target;

    GitLabSCMHeadLabel(String label, SCMHead target) {
        super(label);
        this.target = target;
    }

    final SCMHead getTarget() {
        return target;
    }


    private static class AutomaticBuildLabel extends GitLabSCMHeadLabel {
        AutomaticBuildLabel(String label, SCMHead target) {
            super(label, target);
        }
    }

    // HACK ALERT: the current default buildstrategy of MultiBranchProject does not build TagSCMHead instances automatically;
    // when our sources are observed by another Project which uses different buildstrategies this might not work...
    private static class ManualBuildLabel extends GitLabSCMHeadLabel implements TagSCMHead {
        ManualBuildLabel(String label, SCMHead target) {
            super(label, target);
        }

        @Override
        public long getTimestamp() {
            return 0;
        }
    }
}
