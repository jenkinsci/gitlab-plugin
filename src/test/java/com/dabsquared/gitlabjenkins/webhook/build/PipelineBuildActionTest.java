package com.dabsquared.gitlabjenkins.webhook.build;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PipelineHook;
import hudson.model.FreeStyleProject;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerResponse2;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Milena Zachow
 */
@WithJenkins
@ExtendWith(MockitoExtension.class)
class PipelineBuildActionTest {

    private static JenkinsRule jenkins;

    @Mock
    private StaplerResponse2 response;

    @Mock
    private GitLabPushTrigger trigger;

    private FreeStyleProject testProject;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @BeforeEach
    void setUp() throws Exception {
        testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);
    }

    @Test
    void buildOnSuccess() {
        assertThrows(HttpResponses.HttpResponseException.class, () -> {
            new PipelineBuildAction(testProject, getJson("PipelineEvent.json"), null).execute(response);

            verify(trigger).onPost(any(PipelineHook.class));
        });
    }

    @Test
    void doNotBuildOnFailure() {
        assertThrows(HttpResponses.HttpResponseException.class, () -> {
            new PipelineBuildAction(testProject, getJson("PipelineFailureEvent.json"), null).execute(response);

            verify(trigger, never()).onPost(any(PipelineHook.class));
        });
    }

    private String getJson(String name) throws Exception {
        return IOUtils.toString(getClass().getResourceAsStream(name), StandardCharsets.UTF_8);
    }
}
