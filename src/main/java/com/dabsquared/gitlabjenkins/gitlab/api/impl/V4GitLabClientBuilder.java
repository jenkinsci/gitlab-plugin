package com.dabsquared.gitlabjenkins.gitlab.api.impl;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApi.ApiVersion;
import org.gitlab4j.api.GitLabApiException;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Extension
@Restricted(NoExternalUse.class)
public final class V4GitLabClientBuilder extends GitLabClientBuilder {

    public V4GitLabClientBuilder() {
        super("V4", 1);
    }

    @Override
    @NonNull
    public GitLabApi buildClient(
            String url, String token, boolean ignoreCertificateErrors, int connectionTimeout, int readTimeout) {
        GitLabApi client = null;
        try {
            client = new GitLabApi(ApiVersion.V4, url, token);
            client.setIgnoreCertificateErrors(ignoreCertificateErrors);
            /* whenever using this line of commented code, client is giving gitlabapiexception */
            // client.withRequestTimeout(connectionTimeout, readTimeout);
            client.getUserApi().getCurrentUser(); // for checking if the client is working or not
            return client;
        } catch (GitLabApiException e) {
            return null;
        }
    }
}
