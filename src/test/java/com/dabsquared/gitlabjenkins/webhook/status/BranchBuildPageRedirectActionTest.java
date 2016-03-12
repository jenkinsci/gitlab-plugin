package com.dabsquared.gitlabjenkins.webhook.status;

import hudson.model.FreeStyleProject;

/**
 * @author Robin Müller
 */
public class BranchBuildPageRedirectActionTest extends BuildPageRedirectActionTest {
    @Override
    protected BuildPageRedirectAction getBuildPageRedirectAction(FreeStyleProject project) {
        return new BranchBuildPageRedirectAction(project, branch);
    }
}
