package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import hudson.Extension;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;


@Extension
@Restricted(NoExternalUse.class)
public final class V4GitLabClientBuilder extends ResteasyGitLabClientBuilder {

    public V4GitLabClientBuilder() {
        super(V4GitLabApiProxy.ID, V4GitLabApiProxy.class);
    }
}
