package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import hudson.Extension;


@Extension
public final class V4GitLabClientBuilder extends ResteasyGitLabClientBuilder {
    static final String ID = "v4";

    public V4GitLabClientBuilder() {
        super(ID, V4GitLabClientProxy.class);
    }
}
