package com.dabsquared.gitlabjenkins.testing.gitlab.rule;

import com.dabsquared.gitlabjenkins.gitlab.JacksonConfig;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.api.model.User;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
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

    private final GitLabApi client;
    private final String username;
    private final String password;

    private List<String> projectIds = new ArrayList<>();

    public GitLabRule(String url, int postgresPort) {
        client = new ResteasyClientBuilder()
            .httpEngine(new ApacheHttpClient4Engine())
            .register(new JacksonJsonProvider())
            .register(new JacksonConfig())
            .register(new ApiHeaderTokenFilter(getApiToken(postgresPort))).build().target(url)
            .proxyBuilder(GitLabApi.class)
            .build();
        User user = client.getCurrentUser();
        username = user.getUsername();
        password = "integration-test";
        client.updateUser(user.getId().toString(), user.getEmail(), user.getUsername(), user.getName(), password);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new GitlabStatement(base);
    }

    public String createProject(ProjectRequest request) {
        Project project = client.createProject(request.getName());
        projectIds.add(project.getId().toString());
        if (request.getWebHookUrl() != null && (request.isPushHook() || request.isMergeRequestHook())) {
            client.addProjectHook(project.getId().toString(), request.getWebHookUrl(), request.isPushHook(), request.isMergeRequestHook());
        }
        return project.getHttpUrlToRepo();
    }

    public void createMergeRequest(final Integer projectId,
                                   final String sourceBranch,
                                   final String targetBranch,
                                   final String title) {
        client.createMergeRequest(projectId, sourceBranch, targetBranch, title);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    private void cleanup() {
        for (String projectId : projectIds) {
            String randomProjectName = UUID.randomUUID().toString();
            // rename the project before deleting as the deletion will take a while
            client.updateProject(projectId, randomProjectName, randomProjectName);
            client.deleteProject(projectId);
        }
    }

    private String getApiToken(int postgresPort) {
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

    private static class ApiHeaderTokenFilter implements ClientRequestFilter {
        private final String gitlabApiToken;

        ApiHeaderTokenFilter(String gitlabApiToken) {
            this.gitlabApiToken = gitlabApiToken;
        }

        public void filter(ClientRequestContext requestContext) throws IOException {
            requestContext.getHeaders().putSingle("PRIVATE-TOKEN", gitlabApiToken);
        }
    }
}
