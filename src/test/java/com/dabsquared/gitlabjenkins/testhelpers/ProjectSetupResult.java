package com.dabsquared.gitlabjenkins.testhelpers;

import hudson.model.FreeStyleProject;

public class ProjectSetupResult {

    private FreeStyleProject testProject;
    private BuildNotifier buildNotifier;

    public ProjectSetupResult(FreeStyleProject testProject, BuildNotifier buildNotifier) {
        this.testProject = testProject;
        this.buildNotifier = buildNotifier;
    }

    public FreeStyleProject getTestProject() {
        return testProject;
    }

    public BuildNotifier getBuildNotifier() {
        return buildNotifier;
    }

}
