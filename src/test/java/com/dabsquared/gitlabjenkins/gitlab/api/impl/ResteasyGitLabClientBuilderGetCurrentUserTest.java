package com.dabsquared.gitlabjenkins.gitlab.api.impl;

import static com.dabsquared.gitlabjenkins.gitlab.api.impl.TestUtility.addGitLabApiToken;
import static com.dabsquared.gitlabjenkins.gitlab.api.impl.TestUtility.buildClientWithDefaults;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import com.dabsquared.gitlabjenkins.gitlab.api.model.User;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;

@WithJenkins
@ExtendWith(MockServerExtension.class)
class ResteasyGitLabClientBuilderGetCurrentUserTest {

    private static MockServerClient mockServerClient;

    private GitLabClient client;

    @BeforeAll
    static void setUpMockServer(MockServerClient mockServer) {
        mockServerClient = mockServer;
    }

    @BeforeEach
    void setUp(@SuppressWarnings("unused") JenkinsRule rule) throws Exception {
        addGitLabApiToken();

        String gitLabUrl = "http://localhost:" + mockServerClient.getPort() + "/gitlab";
        GitLabClientBuilder clientBuilder = new ResteasyGitLabClientBuilder("test", 0, V4GitLabApiProxy.class, null);
        client = buildClientWithDefaults(clientBuilder, gitLabUrl);
    }

    @AfterEach
    void tearDown() {
        mockServerClient.reset();
    }

    @Test
    void getCurrentUser_deserializesJsonIntoUser() {
        String userJson = """
            {
              "id": 42,
              "name": "Jen Kins",
              "username": "jenkins",
              "email": "jenkins@noreply.gitlab.example.com"
            }""";

        mockServerClient
                .when(request()
                        .withMethod(HttpMethod.GET)
                        .withPath("/gitlab/api/v4/user")
                        .withHeader("PRIVATE-TOKEN", TestUtility.API_TOKEN))
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(userJson));

        User user = client.getCurrentUser();

        assertEquals(42, user.getId());
        assertEquals("Jen Kins", user.getName());
        assertEquals("jenkins", user.getUsername());
        assertEquals("jenkins@noreply.gitlab.example.com", user.getEmail());
    }

    @Test
    void getCurrentUser_ignoresUnknownJsonFields() {
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

        mockServerClient
                .when(request()
                        .withMethod(HttpMethod.GET)
                        .withPath("/gitlab/api/v4/user")
                        .withHeader("PRIVATE-TOKEN", TestUtility.API_TOKEN))
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(userJson));

        User user = client.getCurrentUser();

        assertEquals(42, user.getId());
        assertEquals("Jen Kins", user.getName());
        assertEquals("jenkins", user.getUsername());
        assertEquals("jenkins@noreply.gitlab.example.com", user.getEmail());
    }
}
