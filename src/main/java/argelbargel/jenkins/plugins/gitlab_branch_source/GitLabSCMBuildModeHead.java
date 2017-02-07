package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.scm.api.mixin.TagSCMHead;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMBuildModeHead.BuildMode.*;


abstract class GitLabSCMBuildModeHead extends GitLabSCMHead {
    enum BuildMode {
        AUTOMATIC,
        MANUAL
    }

    static GitLabSCMBuildModeHead determineBuildMode(GitLabSCMHead head) {
        BuildMode mode = (head instanceof GitLabSCMBranchHead) && ((GitLabSCMBranchHead) head).hasMergeRequest() ? MANUAL : AUTOMATIC;
        return withBuildMode(head, mode);
    }

    static GitLabSCMBuildModeHead withBuildMode(GitLabSCMHead head, BuildMode mode) {
        return (mode == AUTOMATIC) ? new AutomaticBuild(head.getName(), head) : new ManualBuild(head.getName(), head);
    }

    private final GitLabSCMHead target;

    private GitLabSCMBuildModeHead(String label, GitLabSCMHead target) {
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

    private static class AutomaticBuild extends GitLabSCMBuildModeHead {
        AutomaticBuild(String label, GitLabSCMHead target) {
            super(label, target);
        }
    }

    // HACK ALERT: the current default buildstrategy of MultiBranchProject does not build TagSCMHead instances automatically;
    // when our sources are observed by another Project which uses different buildstrategies this might not work...
    private static class ManualBuild extends GitLabSCMBuildModeHead implements TagSCMHead {
        ManualBuild(String label, GitLabSCMHead target) {
            super(label, target);
        }

        @Override
        public long getTimestamp() {
            return 0;
        }
    }
}
