package com.dabsquared.gitlabjenkins.gitlab.api.impl;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import java.util.ArrayList;
import java.util.Collection;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApi.ApiVersion;
import org.gitlab4j.api.GitLabApiException;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Extension
@Restricted(NoExternalUse.class)
public final class AutodetectGitLabClientBuilder extends GitLabClientBuilder {
    public AutodetectGitLabClientBuilder() {
        super("autodetect", 0);
    }

    @Override
    @NonNull
    public GitLabApi buildClient(
            String url, String token, boolean ignoreCertificateErrors, int connectionTimeout, int readTimeout) {
        Collection<GitLabClientBuilder> candidates = new ArrayList<>(getAllGitLabClientBuilders());
        candidates.remove(this);
        GitLabApi client = null;
        for (ApiVersion version : ApiVersion.values()) {
            try {
                client = new GitLabApi(version, url, token);
                client.getUserApi().getCurrentUser();
                break;
            } catch (GitLabApiException e) {
                client = null;
            }
        }
        if (client == null) {
            throw new IllegalArgumentException("Could not autodetect GitLab API version");
        } else {
            client.setIgnoreCertificateErrors(ignoreCertificateErrors);
            // client.withRequestTimeout(connectionTimeout, readTimeout);
            return client;
        }
    }
}
