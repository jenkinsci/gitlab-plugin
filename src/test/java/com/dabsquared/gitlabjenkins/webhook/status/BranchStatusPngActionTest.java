package com.dabsquared.gitlabjenkins.webhook.status;

import hudson.model.FreeStyleProject;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Robin Müller
 */
@WithJenkins
@ExtendWith(MockitoExtension.class)
class BranchStatusPngActionTest extends StatusPngActionTest {

    @Override
    protected BuildStatusAction getBuildStatusAction(FreeStyleProject project) {
        return new BranchStatusPngAction(project, branch);
    }
}
