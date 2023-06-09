package com.dabsquared.gitlabjenkins.gitlab.api.impl;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import hudson.Extension;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApi.ApiVersion;
import org.gitlab4j.api.GitLabApiException;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Extension
@Restricted(NoExternalUse.class)
public final class V3GitLabClientBuilder extends GitLabClientBuilder {

    public V3GitLabClientBuilder() {
        super("V3", 2);
    }

    @Override
    public GitLabApi buildClient(
            String url, String token, boolean ignoreCertificateErrors, int connectionTimeout, int readTimeout) {
        GitLabApi client = null;
        try {
            client = new GitLabApi(ApiVersion.V3, url, token);
            client.getUserApi().getCurrentUser();
            client.setIgnoreCertificateErrors(ignoreCertificateErrors);
            client.setRequestTimeout(connectionTimeout, readTimeout);
            return client;
        } catch (GitLabApiException e) {
            return null;
        }
    }
}
