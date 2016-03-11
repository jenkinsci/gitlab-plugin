package com.dabsquared.gitlabjenkins.webhook.status;

import hudson.model.FreeStyleProject;

/**
 * @author Robin Müller
 */
public class BranchStatusPngActionTest extends StatusPngActionTest {
    @Override
    protected BuildStatusAction getBuildStatusAction(FreeStyleProject project) {
        return new BranchStatusPngAction(project, branch);
    }
}
