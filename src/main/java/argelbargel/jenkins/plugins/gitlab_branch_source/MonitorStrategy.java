package argelbargel.jenkins.plugins.gitlab_branch_source;

class MonitorStrategy {
    private boolean monitor;
    private boolean merged;
    private boolean unmerged;
    private boolean ignoreWIP;
    private boolean onlyMergeable;
    private boolean publishBuildStatus;


    MonitorStrategy(boolean monitor, boolean merge, boolean publish) {
        this.monitor = monitor;
        this.merged = merge;
        this.unmerged = !merge;
        this.ignoreWIP = true;
        this.onlyMergeable = false;
        this.publishBuildStatus = publish;
    }

    void setMonitored(boolean value) {
        monitor = value;
    }

    boolean monitored() {
        return monitor;
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

    void setIgnoreWorkInProgress(boolean value) {
        ignoreWIP = value;
    }

    boolean ignoreWorkInProgress() {
        return ignoreWIP;
    }

    void setBuildOnlyMergeableRequestsMerged(boolean value) {
        onlyMergeable = value;
    }

    boolean buildOnlyMergeableRequestsMerged() {
        return onlyMergeable;
    }

    void setPublishBuildStatus(boolean value) {
        publishBuildStatus = value;
    }

    boolean getPublishBuildStatus() {
        return publishBuildStatus;
    }
}
