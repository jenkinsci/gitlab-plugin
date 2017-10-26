package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import hudson.Extension;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;


@Extension
@Restricted(NoExternalUse.class)
public final class V3GitLabClientBuilder extends ResteasyGitLabClientBuilder {

    public V3GitLabClientBuilder() {
        super(V3GitLabApiProxy.ID, V3GitLabApiProxy.class);
    }
}
