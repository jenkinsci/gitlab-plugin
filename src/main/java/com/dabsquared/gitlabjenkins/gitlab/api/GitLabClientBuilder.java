package com.dabsquared.gitlabjenkins.gitlab.api;


import hudson.ExtensionPoint;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static java.util.Collections.sort;


public abstract class GitLabClientBuilder implements Comparable<GitLabClientBuilder>, ExtensionPoint, Serializable {
    public static GitLabClientBuilder getGitLabClientBuilderById(String id) {
        for (GitLabClientBuilder provider : getAllGitLabClientBuilders()) {
            if (provider.id().equals(id)) {
                return provider;
            }
        }

        throw new NoSuchElementException("unknown client-builder-id: " + id);
    }

    public static List<GitLabClientBuilder> getAllGitLabClientBuilders() {
        List<GitLabClientBuilder> builders = new ArrayList<>(Jenkins.getInstance().getExtensionList(GitLabClientBuilder.class));
        sort(builders);
        return builders;
    }

    private final String id;

    protected GitLabClientBuilder(String id) {
        this.id = id;
    }

    @Nonnull
    public final String id() {
        return id;
    }

    @Nonnull
    public abstract GitLabClient buildClient(String url, String token, boolean ignoreCertificateErrors, int connectionTimeout, int readTimeout);

    @Override
    public final int compareTo(@Nonnull GitLabClientBuilder other) {
        return id().compareTo(other.id());
    }
}
