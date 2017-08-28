package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.List;

import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


class TestUtility {
    static final String API_TOKEN = "secret";
    static final String API_TOKEN_ID = "apiTokenId";
    static final boolean IGNORE_CERTIFICATE_ERRORS = true;
    static final int CONNECTION_TIMEOUT = 10;
    static final int READ_TIMEOUT = 10;

    static void addGitLabApiToken() throws IOException {
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstance())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(domains.get(0),
                    new StringCredentialsImpl(CredentialsScope.SYSTEM, API_TOKEN_ID, "GitLab API Token", Secret.fromString(API_TOKEN)));
            }
        }
    }

    static HttpRequest versionRequest(String id) {
        return request().withMethod(HEAD).withPath("/gitlab/api/" + id + "/.*").withHeader("PRIVATE-TOKEN", API_TOKEN);
    }

    static HttpResponse responseOk() {
        return responseWithStatus(OK);
    }

    static HttpResponse responseNotFound() {
        return responseWithStatus(NOT_FOUND);
    }

    static HttpResponse responseUnauthorized() {
        return responseWithStatus(UNAUTHORIZED);
    }

    static HttpResponse responseWithStatus(Status status) {
        return response().withStatusCode(status.getStatusCode());
    }

    static GitLabClient buildClientWithDefaults(GitLabClientBuilder clientBuilder, String url) {
        return clientBuilder.buildClient(url, API_TOKEN, IGNORE_CERTIFICATE_ERRORS, CONNECTION_TIMEOUT, READ_TIMEOUT);
    }

    private TestUtility() { /* utility class */ }
}
