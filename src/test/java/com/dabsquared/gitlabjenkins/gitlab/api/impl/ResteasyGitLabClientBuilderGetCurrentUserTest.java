package com.dabsquared.gitlabjenkins.gitlab.api.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.dabsquared.gitlabjenkins.connection.GitlabCredentialResolver;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.User;
import hudson.util.Secret;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.RealJenkinsExtension;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;

@ExtendWith(MockServerExtension.class)
class ResteasyGitLabClientBuilderGetCurrentUserTest {

    private static final String API_TOKEN = "secret";
    private static final String API_TOKEN_ID = "apiTokenId";

    private static MockServerClient mockServerClient;

    @RegisterExtension
    private final RealJenkinsExtension rule = new RealJenkinsExtension();

    @BeforeAll
    static void setUpMockServer(MockServerClient mockServer) {
        mockServerClient = mockServer;
    }

    @AfterEach
    void tearDown() {
        mockServerClient.reset();
    }

    @Test
    void getCurrentUser_deserializesJsonIntoUser() throws Throwable {
        String userJson = """
            {
              "id": 42,
              "name": "Jen Kins",
              "username": "jenkins",
              "email": "jenkins@noreply.gitlab.example.com"
            }""";

        setupMockServerClient(userJson);

        rule.then(new DeserializesJsonIntoUserStep(mockServerClient.getPort(), user -> {
            assertEquals(42, user.getId());
            assertEquals("Jen Kins", user.getName());
            assertEquals("jenkins", user.getUsername());
            assertEquals("jenkins@noreply.gitlab.example.com", user.getEmail());
        }));
    }

    @Test
    void getCurrentUser_ignoresUnknownJsonFields() throws Throwable {
        String userJson = """
            {
              "id": 42,
              "username": "jenkins",
              "public_email": null,
              "name": "Jen Kins",
              "state": "active",
              "locked": false,
              "avatar_url": "",
              "web_url": "https://gitlab.example.com/jenkins",
              "created_at": "2026-03-30T14:08:23.172Z",
              "bio": "",
              "location": "",
              "linkedin": "",
              "twitter": "",
              "discord": "",
              "website_url": "",
              "github": "",
              "job_title": "",
              "pronouns": null,
              "organization": "",
              "bot": true,
              "work_information": null,
              "local_time": null,
              "last_sign_in_at": null,
              "confirmed_at": "2026-03-30T14:08:23.172Z",
              "last_activity_on": "2026-03-30",
              "email": "jenkins@noreply.gitlab.example.com",
              "theme_id": 3,
              "color_scheme_id": 1,
              "projects_limit": 10,
              "current_sign_in_at": null,
              "identities": [],
              "can_create_group": true,
              "can_create_project": true,
              "two_factor_enabled": false,
              "external": false,
              "private_profile": false,
              "commit_email": "jenkins@noreply.gitlab.example.com",
              "preferred_language": "en"
            }""";

        setupMockServerClient(userJson);

        rule.then(new DeserializesJsonIntoUserStep(mockServerClient.getPort(), user -> {
            assertEquals(42, user.getId());
            assertEquals("Jen Kins", user.getName());
            assertEquals("jenkins", user.getUsername());
            assertEquals("jenkins@noreply.gitlab.example.com", user.getEmail());
        }));
    }

    private void setupMockServerClient(final String userResponseJson) {
        mockServerClient
                .when(request()
                        .withMethod(HttpMethod.GET)
                        .withPath("/gitlab/api/v4/user")
                        .withHeader("PRIVATE-TOKEN", API_TOKEN))
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(userResponseJson));
    }

    /**
     * Serializable test run class that carries the MockServer port into the Jenkins JVM.
     */
    private static final class DeserializesJsonIntoUserStep implements RealJenkinsExtension.Step {
        @Serial
        private static final long serialVersionUID = 1L;

        private final int port;

        private final SerializableConsumer<User> assertUser;

        public DeserializesJsonIntoUserStep(final int port, final SerializableConsumer<User> assertUser) {
            this.port = port;
            this.assertUser = assertUser;
        }

        private int getPort() {
            return port;
        }

        @Override
        public void run(JenkinsRule r) throws Throwable {
            addGitLabApiToken();

            User user = buildGitLabClient(getPort()).getCurrentUser();

            assertUser.accept(user);
        }

        /**
         * Own version of this utility method, because with {@link RealJenkinsExtension} the test class runs in a
         * different classloader than the plugin code.
         */
        static void addGitLabApiToken() throws Exception {
            for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstanceOrNull())) {
                if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                    List<Domain> domains = credentialsStore.getDomains();
                    credentialsStore.addCredentials(
                            domains.get(0),
                            new StringCredentialsImpl(
                                    CredentialsScope.SYSTEM,
                                    API_TOKEN_ID,
                                    "GitLab API Token",
                                    Secret.fromString(API_TOKEN)));
                }
            }
        }

        /**
         * Own version of this utility method, because with {@link RealJenkinsExtension} the test class runs in a
         * different classloader than the plugin code.
         */
        static GitLabClient buildGitLabClient(final int port) {
            String gitLabUrl = "http://localhost:" + port + "/gitlab";
            return new V4GitLabClientBuilder()
                    .buildClient(gitLabUrl, new GitlabCredentialResolver(null, API_TOKEN_ID), true, 10, 10);
        }
    }

    interface SerializableConsumer<T> extends Consumer<T>, Serializable {}
}
