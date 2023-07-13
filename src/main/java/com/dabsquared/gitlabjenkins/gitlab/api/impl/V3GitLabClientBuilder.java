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
public final class V3GitLabClientBuilder extends GitLabClientBuilder {

    private static final int ORDINAL = 2;

    public V3GitLabClientBuilder() {
        super("V3", ORDINAL);
    }

    @Override
    public GitLabApi buildClient(
            String url, String token, boolean ignoreCertificateErrors, int connectionTimeout, int readTimeout) {
        GitLabApi client = new GitLabApi(ApiVersion.V3, url, token);
            client.setIgnoreCertificateErrors(ignoreCertificateErrors);
            client.setRequestTimeout(connectionTimeout, readTimeout);
        return client;
    }
}
