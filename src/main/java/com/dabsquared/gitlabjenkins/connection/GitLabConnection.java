package com.dabsquared.gitlabjenkins.connection;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * @author Robin Müller
 */
public class GitLabConnection {

    private final String name;
    private final String url;
    private transient String apiToken;
    // TODO make final when migration code gets removed
    private String apiTokenId;
    private final boolean ignoreCertificateErrors;
    private final Integer connectionTimeout;
    private final Integer readTimeout;

    @DataBoundConstructor
    public GitLabConnection(String name, String url, String apiTokenId, boolean ignoreCertificateErrors, Integer connectionTimeout, Integer readTimeout) {
        this.name = name;
        this.url = url;
        this.apiTokenId = apiTokenId;
        this.ignoreCertificateErrors = ignoreCertificateErrors;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getApiTokenId() {
        return apiTokenId;
    }

    public boolean isIgnoreCertificateErrors() {
        return ignoreCertificateErrors;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    protected GitLabConnection readResolve() {
        if (connectionTimeout == null || readTimeout == null) {
            return new GitLabConnection(name, url, apiTokenId, ignoreCertificateErrors, 10, 10);
        }
        return this;
    }

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    public static void migrate() throws IOException {
        GitLabConnectionConfig descriptor = (GitLabConnectionConfig) Jenkins.getInstance().getDescriptor(GitLabConnectionConfig.class);
        for (GitLabConnection connection : descriptor.getConnections()) {
            if (connection.apiTokenId == null && connection.apiToken != null) {
                for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstance())) {
                    if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                        List<Domain> domains = credentialsStore.getDomains();
                        connection.apiTokenId = UUID.randomUUID().toString();
                        credentialsStore.addCredentials(domains.get(0),
                            new GitLabApiTokenImpl(CredentialsScope.SYSTEM, connection.apiTokenId, "GitLab API Token", Secret.fromString(connection.apiToken)));
                    }
                }
            }
        }
        descriptor.save();
    }
}
