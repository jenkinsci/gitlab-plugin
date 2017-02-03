package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.scm.api.SCMHead;
import jenkins.scm.api.mixin.TagSCMHead;

/**
 * Changes the name/label of the job for the given head
 */
abstract class GitLabSCMHeadLabel extends GitLabSCMHead {
    static GitLabSCMHeadLabel create(GitLabSCMHead head, boolean automaticBuild) {
        String label = head.getPronoun() + " " + head.getName();
        return (automaticBuild) ? new AutomaticBuildLabel(label, head) : new ManualBuildLabel(label, head);
    }


    private final GitLabSCMHead target;

    GitLabSCMHeadLabel(String label, GitLabSCMHead target) {
        super(label);
        this.target = target;
    }

    @Override
    public GitLabSCMCommit getCommit() {
        return target.getCommit();
    }

    final SCMHead getTarget() {
        return target;
    }


    private static class AutomaticBuildLabel extends GitLabSCMHeadLabel {
        AutomaticBuildLabel(String label, GitLabSCMHead target) {
            super(label, target);
        }
    }

    // HACK ALERT: the current default buildstrategy of MultiBranchProject does not build TagSCMHead instances automatically;
    // when our sources are observed by another Project which uses different buildstrategies this might not work...
    private static class ManualBuildLabel extends GitLabSCMHeadLabel implements TagSCMHead {
        ManualBuildLabel(String label, GitLabSCMHead target) {
            super(label, target);
        }

        @Override
        public long getTimestamp() {
            return 0;
        }
    }
}
