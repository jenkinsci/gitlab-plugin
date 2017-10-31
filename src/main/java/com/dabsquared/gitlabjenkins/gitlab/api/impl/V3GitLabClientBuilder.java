package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import hudson.Extension;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;


@Extension
@Restricted(NoExternalUse.class)
public final class V3GitLabClientBuilder extends ResteasyGitLabClientBuilder {
    private static final int ORDINAL = 2;

    public V3GitLabClientBuilder() {
        super(V3GitLabApiProxy.ID, ORDINAL, V3GitLabApiProxy.class);
    }
}
