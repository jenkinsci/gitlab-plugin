package com.dabsquared.gitlabjenkins.workflow;

import hudson.model.Action;
import java.io.Serializable;
import java.util.List;

/**
 * @author Robin Müller
 */
public class PendingBuildsAction implements Action, Serializable {

    private final List<String> builds;

    public PendingBuildsAction(List<String> builds) {
        this.builds = builds;
    }

    public void startBuild(String name) {
        builds.remove(name);
    }

    public List<String> getBuilds() {
        return builds;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }
}
