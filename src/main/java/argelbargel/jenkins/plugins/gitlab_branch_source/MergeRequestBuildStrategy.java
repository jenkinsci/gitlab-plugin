package argelbargel.jenkins.plugins.gitlab_branch_source;

class MergeRequestBuildStrategy {
    private boolean enabled;
    private boolean merged;
    private boolean unmerged;
    private boolean mergeable;
    private boolean wip;

    MergeRequestBuildStrategy(boolean initalState) {
        enabled = initalState;
        merged = true;
        unmerged = false;
        mergeable = true;
        wip = true;
    }

    void setEnabled(boolean value) {
        enabled = value;
    }

    boolean enabled() {
        return enabled & (merged | unmerged);
    }

    void setBuildMerged(boolean value) {
        merged = value;
    }

    boolean buildMerged() {
        return merged;
    }

    void setBuildUnmerged(boolean value) {
        unmerged = value;
    }

    boolean buildUnmerged() {
        return unmerged;
    }

    void setBuildOnlyMergeable(boolean value) {
        mergeable = value;
    }

    boolean buildOnlyMergeable() {
        return mergeable;
    }

    void setIgnoreWorkInProgress(boolean value) {
        wip = value;
    }

    boolean ignoreWorkInProgress() {
        return wip;
    }
}
