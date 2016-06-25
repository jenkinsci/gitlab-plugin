package com.dabsquared.gitlabjenkins.connection;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.Util;
import hudson.util.Secret;

/**
 * @author Robin Müller
 */
@NameWith(GitLabApiToken.NameProvider.class)
public interface GitLabApiToken extends StandardCredentials {

    Secret getApiToken();

    class NameProvider extends CredentialsNameProvider<GitLabApiToken> {
        @Override
        public String getName(GitLabApiToken c) {
            String description = Util.fixEmptyAndTrim(c.getDescription());
            return Messages.GitLabApiToken_name() + (description != null ? " (" + description + ")" : "");
        }
    }
}
