package com.dabsquared.gitlabjenkins.webhook.status;

import hudson.model.FreeStyleProject;

/**
 * @author Robin Müller
 */
public class CommitStatusPngActionTest extends StatusPngActionTest {
    @Override
    protected BuildStatusAction getBuildStatusAction(FreeStyleProject project) {
        return new CommitStatusPngAction(project, commitSha1);
    }
}
