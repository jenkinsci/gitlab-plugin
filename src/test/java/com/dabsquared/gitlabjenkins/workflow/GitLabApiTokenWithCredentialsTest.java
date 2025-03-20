package com.dabsquared.gitlabjenkins.workflow;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.dabsquared.gitlabjenkins.connection.GitLabApiTokenImpl;
import hudson.model.Run;
import hudson.util.IOUtils;
import hudson.util.Secret;
import java.io.IOException;
import java.util.List;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class GitLabApiTokenWithCredentialsTest {

    private static final String API_TOKEN = "secret";
    private static final String API_TOKEN_ID = "apiTokenId";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void setup() throws IOException {
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(jenkins.jenkins)) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(
                        domains.get(0),
                        new GitLabApiTokenImpl(
                                CredentialsScope.GLOBAL,
                                API_TOKEN_ID,
                                "GitLab API Token",
                                Secret.fromString(API_TOKEN)));
            }
        }
    }

    @Test
    public void withCredentials_success() throws Exception {
        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        String pipelineText =
                IOUtils.toString(getClass().getResourceAsStream("pipeline/withCredentials-pipeline.groovy"));
        project.setDefinition(new CpsFlowDefinition(pipelineText, false));
        Run<?, ?> build = jenkins.buildAndAssertSuccess(project);
        // assert false to know we run it in tests
        jenkins.assertLogContains("Token1 is ecret", build);
        jenkins.assertLogContains("Token2 is ecret", build);
    }
}
