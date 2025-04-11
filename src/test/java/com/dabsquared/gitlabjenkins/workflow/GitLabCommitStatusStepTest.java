package com.dabsquared.gitlabjenkins.workflow;

import hudson.model.Run;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class GitLabCommitStatusStepTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void bare_gitlabCommitStatus() throws Exception {
        WorkflowJob project = j.createProject(WorkflowJob.class);
        String pipelineText = IOUtils.toString(
                getClass().getResourceAsStream("pipeline/bare-gitlabCommitStatus-pipeline.groovy"),
                StandardCharsets.UTF_8);
        project.setDefinition(new CpsFlowDefinition(pipelineText, false));
        Run<?, ?> build = j.buildAndAssertSuccess(project);
        j.assertLogContains("this is simple jenkins-build", build);
    }

    @Test
    void named_simple_pipeline_builds_as_LString() throws Exception {
        WorkflowJob project = j.createProject(WorkflowJob.class);
        String pipelineText = IOUtils.toString(
                getClass().getResourceAsStream("pipeline/named-simple-pipeline-builds-as-LString.groovy"),
                StandardCharsets.UTF_8);
        project.setDefinition(new CpsFlowDefinition(pipelineText, false));
        Run<?, ?> build = j.buildAndAssertSuccess(project);
        j.assertLogContains("this is pre-build stage", build);
    }

    @Test
    void named_simple_pipeline_builds_as_String() throws Exception {
        WorkflowJob project = j.createProject(WorkflowJob.class);
        String pipelineText = IOUtils.toString(
                getClass().getResourceAsStream("pipeline/named-simple-pipeline-builds-as-String.groovy"),
                StandardCharsets.UTF_8);
        project.setDefinition(new CpsFlowDefinition(pipelineText, false));
        Run<?, ?> build = j.buildAndAssertSuccess(project);
        j.assertLogContains("this is pre-build stage", build);
    }

    @Test
    void multisite() throws Exception {
        WorkflowJob project = j.createProject(WorkflowJob.class);
        String pipelineText = IOUtils.toString(
                getClass().getResourceAsStream("pipeline/multisite-pipeline.groovy"), StandardCharsets.UTF_8);
        project.setDefinition(new CpsFlowDefinition(pipelineText, false));
        Run<?, ?> build = j.buildAndAssertSuccess(project);
        j.assertLogContains("this is stage3", build);
    }

    @Test
    void multiproject_specific_connection() throws Exception {
        WorkflowJob project = j.createProject(WorkflowJob.class);
        String pipelineText = IOUtils.toString(
                getClass().getResourceAsStream("pipeline/multiproject-specific-connection-pipeline.groovy"),
                StandardCharsets.UTF_8);
        project.setDefinition(new CpsFlowDefinition(pipelineText, false));
        Run<?, ?> build = j.buildAndAssertSuccess(project);
        j.assertLogContains("this is pre-build stage", build);
    }

    @Test
    void multiproject() throws Exception {
        WorkflowJob project = j.createProject(WorkflowJob.class);
        String pipelineText = IOUtils.toString(
                getClass().getResourceAsStream("pipeline/multiproject-pipeline.groovy"), StandardCharsets.UTF_8);
        project.setDefinition(new CpsFlowDefinition(pipelineText, false));
        Run<?, ?> build = j.buildAndAssertSuccess(project);
        j.assertLogContains("this is pre-build stage", build);
    }
}
