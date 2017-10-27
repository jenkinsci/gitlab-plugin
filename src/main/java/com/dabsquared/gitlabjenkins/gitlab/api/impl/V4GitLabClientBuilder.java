package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import hudson.Extension;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;


@Extension
@Restricted(NoExternalUse.class)
public final class V4GitLabClientBuilder extends ResteasyGitLabClientBuilder {
    private static final int ORDINAL = 1;

    public V4GitLabClientBuilder() {
        super(V4GitLabApiProxy.ID, ORDINAL, V4GitLabApiProxy.class);
    }
}
