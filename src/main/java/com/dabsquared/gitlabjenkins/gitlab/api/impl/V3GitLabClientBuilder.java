package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import hudson.Extension;


@Extension
public final class V3GitLabClientBuilder extends ResteasyGitLabClientBuilder {
    static final String ID = "v3";

    public V3GitLabClientBuilder() {
        super(ID, V3GitLabClientProxy.class);
    }
}
