package com.dabsquared.gitlabjenkins.testing.gitlab.rule;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.dabsquared.gitlabjenkins.connection.GitLabConnection;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.impl.V4GitLabClientBuilder;
import hudson.util.Secret;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Pipeline;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * @author Robin Müller
 */
public class GitLabRule implements TestRule {
    private static final String API_TOKEN_ID = "apiTokenId";
    private static final CharSequence PASSWORD = "integration-test";

    private final String url;
    private final int postgresPort;

    private GitLabApi clientCache;

    private List<String> projectIds = new ArrayList<>();

    public GitLabRule(String url, int postgresPort) {
        this.url = url;
        this.postgresPort = postgresPort;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new GitlabStatement(base);
    }

    public Project getProject(final String projectName) throws GitLabApiException {
        return client().getProjectApi().getProject(projectName);
    }

    public List<Pipeline> getPipelines(Long projectId) throws GitLabApiException {
        return client().getPipelineApi().getPipelines(String.valueOf(projectId));
    }

    public List<String> getProjectIds() {
        return projectIds;
    }

    public String createProject(ProjectRequest request) throws GitLabApiException {
        Project project = client().getProjectApi().createProject(request.getName());
        projectIds.add(project.getId().toString());
        if (request.getWebHookUrl() != null
                && (request.isPushHook() || request.isMergeRequestHook() || request.isNoteHook())) {
            client().getProjectApi()
                    .addHook(
                            project.getId().toString(),
                            request.getWebHookUrl(),
                            request.isPushHook(),
                            request.isMergeRequestHook(),
                            request.isNoteHook());
        }
        return project.getHttpUrlToRepo();
    }

    public GitLabConnectionProperty createGitLabConnectionProperty() throws IOException {
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstance())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(
                        domains.get(0),
                        new StringCredentialsImpl(
                                CredentialsScope.SYSTEM,
                                API_TOKEN_ID,
                                "GitLab API Token",
                                Secret.fromString(getApiToken())));
            }
        }

        GitLabConnectionConfig config = Jenkins.getInstance().getDescriptorByType(GitLabConnectionConfig.class);
        GitLabConnection connection =
                new GitLabConnection("test", url, API_TOKEN_ID, new V4GitLabClientBuilder(), true, 10, 10);
        config.addConnection(connection);
        config.save();
        return new GitLabConnectionProperty(connection.getName());
    }

    public MergeRequest createMergeRequest(
            final Long projectId, final String sourceBranch, final String targetBranch, final String title)
            throws GitLabApiException {
        return client().getMergeRequestApi().createMergeRequest(projectId, sourceBranch, targetBranch, title, "", null);
    }

    public void createMergeRequestNote(MergeRequest mr, Long mergeRequestIid, String body) throws GitLabApiException {
        client().getNotesApi().createMergeRequestNote(mr, mergeRequestIid, body);
    }

    public String getUsername() throws GitLabApiException {
        return client().getUserApi().getCurrentUser().getUsername();
    }

    public CharSequence getPassword() {
        return PASSWORD;
    }

    private void cleanup() throws GitLabApiException {
        for (String projectId : projectIds) {
            // rename the project before deleting as the deletion will take a while
            Project project = new Project();
            project.setId(Long.parseLong(projectId));
            client().getProjectApi().updateProject(project);
            client().getProjectApi().deleteProject(projectId);
        }
    }

    private String getApiToken() {
        try {
            Class.forName("org.postgresql.Driver");
            try (Connection connection = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:" + postgresPort + "/gitlabhq_production", "gitlab", "password")) {
                ResultSet resultSet = connection
                        .createStatement()
                        .executeQuery("SELECT authentication_token FROM users WHERE username = 'root'");
                resultSet.next();
                return resultSet.getString("authentication_token");
            }
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private GitLabApi client() throws GitLabApiException {
        if (clientCache == null) {
            clientCache = new V4GitLabClientBuilder().buildClient(url, getApiToken(), false, -1, -1);
            User user = clientCache.getUserApi().getCurrentUser();
            client().getUserApi().updateUser(user, PASSWORD);
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
