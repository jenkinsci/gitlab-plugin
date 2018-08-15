package com.dabsquared.gitlabjenkins.workflow;


import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.Run;
import org.jvnet.hudson.test.JenkinsRule;
import org.junit.Rule;
import org.junit.Test;

public class GitLabCommitStatusStepTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void bare_gitlabCommitStatus() throws Exception {
        WorkflowJob project = j.createProject(WorkflowJob.class);
        String pipelineText =  IOUtils.toString(getClass().getResourceAsStream(
            "pipeline/bare-gitlabCommitStatus-pipeline.groovy"));
        project.setDefinition(new CpsFlowDefinition(pipelineText, false));
        Run build = j.buildAndAssertSuccess(project);
        j.assertLogContains("this is simple jenkins-build", build);
    }

    @Test
    public void simple_pipeline_buils_as_LString() throws Exception {
        WorkflowJob project = j.createProject(WorkflowJob.class);
        String pipelineText =  IOUtils.toString(getClass().getResourceAsStream(
            "pipeline/simple-pipeline-builds-as-LString.groovy"));
        project.setDefinition(new CpsFlowDefinition(pipelineText, false));
        Run build = j.buildAndAssertSuccess(project);
        j.assertLogContains("this is pre-build stage", build);
    }

    @Test
    public void simple_pipeline_buils_as_String() throws Exception {
        WorkflowJob project = j.createProject(WorkflowJob.class);
        String pipelineText =  IOUtils.toString(getClass().getResourceAsStream(
            "pipeline/simple-pipeline-builds-as-String.groovy"));
        project.setDefinition(new CpsFlowDefinition(pipelineText, false));
        Run build = j.buildAndAssertSuccess(project);
        j.assertLogContains("this is pre-build stage", build);
    }

    @Test
    public void named_simple_pipeline_buils_as_LString() throws Exception {
        WorkflowJob project = j.createProject(WorkflowJob.class);
        String pipelineText =  IOUtils.toString(getClass().getResourceAsStream(
            "pipeline/named-simple-pipeline-builds-as-LString.groovy"));
        project.setDefinition(new CpsFlowDefinition(pipelineText, false));
        Run build = j.buildAndAssertSuccess(project);
        j.assertLogContains("this is pre-build stage", build);
    }

    @Test
    public void named_simple_pipeline_buils_as_String() throws Exception {
        WorkflowJob project = j.createProject(WorkflowJob.class);
        String pipelineText =  IOUtils.toString(getClass().getResourceAsStream(
            "pipeline/named-simple-pipeline-builds-as-String.groovy"));
        project.setDefinition(new CpsFlowDefinition(pipelineText, false));
        Run build = j.buildAndAssertSuccess(project);
        j.assertLogContains("this is pre-build stage", build);
    }

    @Test
    public void multisite() throws Exception {
        WorkflowJob project = j.createProject(WorkflowJob.class);
        String pipelineText =  IOUtils.toString(getClass().getResourceAsStream(
            "pipeline/multisite-pipeline.groovy"));
        project.setDefinition(new CpsFlowDefinition(pipelineText, false));
        Run build = j.buildAndAssertSuccess(project);
        j.assertLogContains("this is pre-build stage", build);
    }

    @Test
    public void multiproject_specific_connection() throws Exception {
        WorkflowJob project = j.createProject(WorkflowJob.class);
        String pipelineText =  IOUtils.toString(getClass().getResourceAsStream(
            "pipeline/multiproject-specific-connection-pipeline.groovy"));
        project.setDefinition(new CpsFlowDefinition(pipelineText, false));
        Run build = j.buildAndAssertSuccess(project);
        j.assertLogContains("this is pre-build stage", build);
    }

    @Test
    public void multiproject() throws Exception {
        WorkflowJob project = j.createProject(WorkflowJob.class);
        String pipelineText =  IOUtils.toString(getClass().getResourceAsStream(
            "pipeline/multiproject-pipeline.groovy"));
        project.setDefinition(new CpsFlowDefinition(pipelineText, false));
        Run build = j.buildAndAssertSuccess(project);
        j.assertLogContains("this is pre-build stage", build);
    }
}
