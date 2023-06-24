package com.dabsquared.gitlabjenkins.gitlab.api.impl;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import org.gitlab4j.api.GitLabApi;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Extension
@Restricted(NoExternalUse.class)
public final class AutodetectGitLabClientBuilder extends GitLabClientBuilder {
    private static final int ORDINAL = 0;

    public AutodetectGitLabClientBuilder() {
        super("autodetect", ORDINAL);
    }

    @Override
    @NonNull
    public GitLabApi buildClient(
            String url, String token, boolean ignoreCertificateErrors, int connectionTimeout, int readTimeout) {
        Collection<GitLabClientBuilder> candidates = new ArrayList<>(getAllGitLabClientBuilders());
        candidates.remove(this);
        return autodetectOrDie(candidates, url, token, ignoreCertificateErrors, connectionTimeout, readTimeout);
    }

    @NonNull
    private GitLabApi autodetectOrDie(
            Collection<GitLabClientBuilder> candidates,
            String url,
            String token,
            boolean ignoreCertificateErrors,
            int connectionTimeout,
            int readTimeout) {
        GitLabApi client = autodetect(candidates, url, token, ignoreCertificateErrors, connectionTimeout, readTimeout);
        if (client != null) {
            return client;
        }

        throw new NoSuchElementException("no client-builder found that supports server at " + url);
    }

    @CheckForNull
    private GitLabApi autodetect(
            Collection<GitLabClientBuilder> candidates,
            String url,
            String token,
            boolean ignoreCertificateErrors,
            int connectionTimeout,
            int readTimeout) {
        for (GitLabClientBuilder candidate : candidates) {
            GitLabApi client =
                    candidate.buildClient(url, token, ignoreCertificateErrors, connectionTimeout, readTimeout);
            return client;
        }

        return null;
    }
}
