package com.dabsquared.gitlabjenkins.gitlab.api.impl;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import hudson.Extension;
import java.util.NoSuchElementException;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApi.ApiVersion;
import org.gitlab4j.api.GitLabApiException;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Extension
@Restricted(NoExternalUse.class)
public final class V4GitLabClientBuilder extends GitLabClientBuilder {

    private static final int ORDINAL = 1;

    public V4GitLabClientBuilder() {
        super("V4", ORDINAL);
    }

    @Override
    public GitLabApi buildClient(
            String url, String token, boolean ignoreCertificateErrors, int connectionTimeout, int readTimeout) {
        GitLabApi client = new GitLabApi(ApiVersion.V4, url, token);
        try {
            client.getUserApi().getCurrentUser();
            client.setIgnoreCertificateErrors(ignoreCertificateErrors);
            client.setRequestTimeout(connectionTimeout, readTimeout);
        } catch (GitLabApiException e) {
            throw new NoSuchElementException("no client-builder found that supports server at " + url);
        }
        return client;
    }
}
