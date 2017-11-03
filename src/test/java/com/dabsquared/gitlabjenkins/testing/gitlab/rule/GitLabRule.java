package com.dabsquared.gitlabjenkins.testing.gitlab.rule;


import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.dabsquared.gitlabjenkins.connection.GitLabConnection;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.impl.V3GitLabClientBuilder;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Pipeline;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.api.model.User;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Robin Müller
 */
public class GitLabRule implements TestRule {
    private static final String API_TOKEN_ID = "apiTokenId";
    private static final String PASSWORD = "integration-test";

    private final String url;
    private final int postgresPort;

    private GitLabClient clientCache;

    private List<String> projectIds = new ArrayList<>();

    public GitLabRule(String url, int postgresPort) {
        this.url = url;
        this.postgresPort = postgresPort;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new GitlabStatement(base);
    }

    public Project getProject(final String projectName) {
        return client().getProject(projectName);
    }

    public List<Pipeline> getPipelines(int projectId) {
        return client().getPipelines(String.valueOf(projectId));
    }

    public List<String> getProjectIds() {
        return projectIds;
    }

    public String createProject(ProjectRequest request) {
        Project project = client().createProject(request.getName());
        projectIds.add(project.getId().toString());
        if (request.getWebHookUrl() != null && (request.isPushHook() || request.isMergeRequestHook() || request.isNoteHook())) {
            client().addProjectHook(project.getId().toString(), request.getWebHookUrl(), request.isPushHook(), request.isMergeRequestHook(), request.isNoteHook());
        }
        return project.getHttpUrlToRepo();
    }

    public GitLabConnectionProperty createGitLabConnectionProperty() throws IOException {
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstance())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(domains.get(0),
                    new StringCredentialsImpl(CredentialsScope.SYSTEM, API_TOKEN_ID, "GitLab API Token", Secret.fromString(getApiToken())));
            }
        }

        GitLabConnectionConfig config = Jenkins.getInstance().getDescriptorByType(GitLabConnectionConfig.class);
        GitLabConnection connection = new GitLabConnection("test", url, API_TOKEN_ID, new V3GitLabClientBuilder(), true,10, 10);
        config.addConnection(connection);
        config.save();
        return new GitLabConnectionProperty(connection.getName());
    }

    public MergeRequest createMergeRequest(final Integer projectId,
                                           final String sourceBranch,
                                           final String targetBranch,
                                           final String title) {
        return client().createMergeRequest(projectId, sourceBranch, targetBranch, title);
    }

    public void createMergeRequestNote(MergeRequest mr, String body) {
        client().createMergeRequestNote(mr, body);
    }

    public String getUsername() {
        return client().getCurrentUser().getUsername();
    }

    public String getPassword() {
        return PASSWORD;
    }

    private void cleanup() {
        for (String projectId : projectIds) {
            String randomProjectName = UUID.randomUUID().toString();
            // rename the project before deleting as the deletion will take a while
            client().updateProject(projectId, randomProjectName, randomProjectName);
            client().deleteProject(projectId);
        }
    }

    private String getApiToken() {
        try {
            Class.forName("org.postgresql.Driver");
            try (Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:" + postgresPort + "/gitlabhq_production", "gitlab", "password")) {
                ResultSet resultSet = connection.createStatement().executeQuery("SELECT authentication_token FROM users WHERE username = 'root'");
                resultSet.next();
                return resultSet.getString("authentication_token");
            }
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private GitLabClient client() {
        if (clientCache == null) {
            clientCache = new V3GitLabClientBuilder().buildClient(url, getApiToken(), false, -1, -1);
            User user = clientCache.getCurrentUser();
            client().updateUser(user.getId().toString(), user.getEmail(), user.getUsername(), user.getName(), PASSWORD);
        }
        return clientCache;
    }

    private class GitlabStatement extends Statement {
        private final Statement next;

        private GitlabStatement(Statement next) {
            this.next = next;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                next.evaluate();
            } finally {
                GitLabRule.this.cleanup();
            }
        }
    }
}
