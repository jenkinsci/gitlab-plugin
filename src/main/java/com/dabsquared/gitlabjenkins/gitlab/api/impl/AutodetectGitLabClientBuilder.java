package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import hudson.Extension;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;


@Extension
@Restricted(NoExternalUse.class)
public final class AutodetectGitLabClientBuilder extends GitLabClientBuilder {
    public AutodetectGitLabClientBuilder() {
        super("autodetect", 0);
    }

    @Override
    @Nonnull
    public GitLabClient buildClient(String url, String token, boolean ignoreCertificateErrors, int connectionTimeout, int readTimeout) {
        Collection<GitLabClientBuilder> candidates = new ArrayList<>(getAllGitLabClientBuilders());
        candidates.remove(this);
        return new AutodetectingGitLabClient(candidates, url, token, ignoreCertificateErrors, connectionTimeout, readTimeout);
    }
}
