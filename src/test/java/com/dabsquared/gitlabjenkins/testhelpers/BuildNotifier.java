package com.dabsquared.gitlabjenkins.testhelpers;

import com.dabsquared.gitlabjenkins.trigger.handler.merge.LockWrapper;
import hudson.model.AbstractBuild;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BuildNotifier {
    private final LockWrapper lock;
    private List<AbstractBuild> builds;

    public BuildNotifier(final LockWrapper lock) {
        this.lock = lock;
        builds = new ArrayList<>();
    }

    public int getNumberOfBuildsTriggered() {
        return builds.size();
    }

    public List<AbstractBuild> getBuildsTriggered() {
        return Collections.unmodifiableList(builds);
    }

    public LockWrapper getLock() {
        return lock;
    }

    public void addBuild(AbstractBuild<?, ?> build) {
        builds.add(build);
    }

    public AbstractBuild getLastTriggeredBuild() {
        return builds.isEmpty() ? null : builds.get(builds.size()-1);
    }
}
