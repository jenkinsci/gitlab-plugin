package com.dabsquared.gitlabjenkins.workflow;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;

import com.dabsquared.gitlabjenkins.connection.GitLabConnection;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import com.dabsquared.gitlabjenkins.gitlab.api.impl.V4GitLabClientBuilder;

import hudson.model.Run;
import hudson.util.Secret;

import java.util.List;
import org.apache.commons.io.IOUtils;

import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RealJenkinsRule;

import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;

public class AcceptGitLabMergeRequestStepTest {

    @Rule public RealJenkinsRule rr = new RealJenkinsRule();

    @Rule public MockServerRule mockServer = new MockServerRule(new Object());

    private MockServerClient mockServerClient;

    @Before
    public void setup() {
        mockServerClient = new MockServerClient("localhost", mockServer.getPort());
    }

    @After
    public void cleanup() {
        mockServerClient.reset();
    }

    @Test
    public void acceptGitlabMergeRequest() throws Throwable {
        int port = mockServer.getPort();
        String pipelineText =
                IOUtils.toString(
                        getClass().getResourceAsStream("pipeline/acceptGitlabMergeRequest.groovy"));
        rr.then(r -> {
            _acceptGitlabMergeRequest(r, port, pipelineText);
        });
    }
    
    private static void _acceptGitlabMergeRequest(JenkinsRule r, int port, String pipelineText)
            throws Throwable {
        setupGitLabConnections(r, port);
        WorkflowJob project = r.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(pipelineText, false));
        Run build = r.buildAndAssertSuccess(project);
        r.assertLogContains("this is simple jenkins-build", build);
    }
    
    private static void setupGitLabConnections(JenkinsRule r, int port) throws Throwable {
        GitLabConnectionConfig connectionConfig = r.get(GitLabConnectionConfig.class);
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(r.jenkins)) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(
                        domains.get(0),
                        new StringCredentialsImpl(
                                CredentialsScope.SYSTEM,
                                "apiTokenId",
                                "GitLab API Token",
                                Secret.fromString("secret")));
            }
        }
        connectionConfig.addConnection(
                new GitLabConnection(
                        "test-connection",
                        "http://localhost:" + port + "/gitlab",
                        "apiTokenId",
                        new V4GitLabClientBuilder(),
                        false,
                        10,
                        10));
    }
}
    
