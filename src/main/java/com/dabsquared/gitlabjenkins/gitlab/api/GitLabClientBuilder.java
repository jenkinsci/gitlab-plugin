package com.dabsquared.gitlabjenkins.gitlab.api;


import hudson.ExtensionPoint;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static java.util.Collections.sort;

@Restricted(NoExternalUse.class)
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
    private final int ordinal;

    protected GitLabClientBuilder(String id, int ordinal) {
        this.id = id;
        this.ordinal = ordinal;
    }

    @Nonnull
    public final String id() {
        return id;
    }

    @Nonnull
    public abstract GitLabClient buildClient(String url, String token, boolean ignoreCertificateErrors, int connectionTimeout, int readTimeout);

    @Override
    public final int compareTo(@Nonnull GitLabClientBuilder other) {
        int o = ordinal - other.ordinal;
        return o != 0 ? o : id().compareTo(other.id());
    }
}
