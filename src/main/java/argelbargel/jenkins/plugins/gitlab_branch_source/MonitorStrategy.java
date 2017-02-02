package argelbargel.jenkins.plugins.gitlab_branch_source;

class MonitorStrategy {
    private boolean monitor;
    private boolean merged;
    private boolean unmerged;
    private boolean ignoreWIP;

    MonitorStrategy(boolean monitor, boolean merge) {
        this.monitor = monitor;
        this.merged = merge;
        this.unmerged = !merge;
        ignoreWIP = true;
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
}
