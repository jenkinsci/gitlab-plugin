package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import hudson.Extension;

import javax.annotation.Nonnull;
import javax.ws.rs.NotFoundException;
import java.util.NoSuchElementException;


@Extension
public final class AutodetectGitLabClientBuilder extends GitLabClientBuilder {
    public AutodetectGitLabClientBuilder() {
        super("autodetect");
    }

    @Override
    @Nonnull
    public GitLabClient buildClient(String url, String token, boolean ignoreCertificateErrors, int connectionTimeout, int readTimeout) {
        return autodetectOrDie(url, token, ignoreCertificateErrors, connectionTimeout, readTimeout);
    }

    private GitLabClient autodetectOrDie(String url, String token, boolean ignoreCertificateErrors, int connectionTimeout, int readTimeout) {
        GitLabClient client = autodetect(url, token, ignoreCertificateErrors, connectionTimeout, readTimeout);
        if (client != null) {
            return client;
        }

        throw new NoSuchElementException("no client-builder found that supports server at " + url);
    }

    private GitLabClient autodetect(String url, String token, boolean ignoreCertificateErrors, int connectionTimeout, int readTimeout) {
        for (GitLabClientBuilder candidate : getAllGitLabClientBuilders()) {
            if (candidate == this) {
                continue; // ignore ourself...
            }
            GitLabClient client = candidate.buildClient(url, token, ignoreCertificateErrors, connectionTimeout, readTimeout);
            try {
                client.headCurrentUser();
                return client;
            } catch (NotFoundException ignored) {
                // api-endpoint not found (== api-level not supported by this client)
            }
        }

        return null;
    }
}
