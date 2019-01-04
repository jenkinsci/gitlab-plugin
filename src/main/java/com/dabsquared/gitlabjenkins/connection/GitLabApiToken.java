package com.dabsquared.gitlabjenkins.connection;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import hudson.Util;
import hudson.util.Secret;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import javax.annotation.Nonnull;

/**
 * @author Robin Müller
 */
@NameWith(GitLabApiToken.NameProvider.class)
public interface GitLabApiToken extends StringCredentials {

    Secret getApiToken();

    @Nonnull
    @Override
    default Secret getSecret() {
        return getApiToken();
    }

    class NameProvider extends CredentialsNameProvider<GitLabApiToken> {
        @Override
        public String getName(GitLabApiToken c) {
            String description = Util.fixEmptyAndTrim(c.getDescription());
            return Messages.GitLabApiToken_name() + (description != null ? " (" + description + ")" : "");
        }
    }
}
