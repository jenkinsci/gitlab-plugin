package com.dabsquared.gitlabjenkins.connection;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.Serial;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

public class GitLabCredentialMatcher implements CredentialsMatcher {

    @Serial
    private static final long serialVersionUID = -6684402077086938070L;

    @Override
    public boolean matches(@NonNull Credentials credentials) {
        try {
            return credentials instanceof GitLabApiToken || credentials instanceof StringCredentials;
        } catch (Throwable e) {
            return false;
        }
    }
}
