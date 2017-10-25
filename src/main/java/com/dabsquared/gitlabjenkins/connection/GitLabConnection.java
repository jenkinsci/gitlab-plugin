package com.dabsquared.gitlabjenkins.connection;


import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import com.dabsquared.gitlabjenkins.gitlab.api.impl.AutodetectGitLabClientBuilder;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder.getGitLabClientBuilderById;


/**
 * @author Robin Müller
 */
public class GitLabConnection {
    private final String name;
    private final String url;
    private transient String apiToken;
    // TODO make final when migration code gets removed
    private String apiTokenId;
    private GitLabClientBuilder clientBuilder;
    private final boolean ignoreCertificateErrors;
    private final Integer connectionTimeout;
    private final Integer readTimeout;
    private transient GitLabClient apiCache;

    public GitLabConnection(String name, String url, String apiTokenId, boolean ignoreCertificateErrors, Integer connectionTimeout, Integer readTimeout) {
        this(
            name,
            url,
            apiTokenId,
            new AutodetectGitLabClientBuilder(),
            ignoreCertificateErrors,
            connectionTimeout,
            readTimeout
        );
    }

    @DataBoundConstructor
    public GitLabConnection(String name, String url, String apiTokenId, String clientBuilderId, boolean ignoreCertificateErrors, Integer connectionTimeout, Integer readTimeout) {
        this(
            name,
            url,
            apiTokenId,
            getGitLabClientBuilderById(clientBuilderId),
            ignoreCertificateErrors,
            connectionTimeout,
            readTimeout
        );
    }

    @Restricted(NoExternalUse.class)
    public GitLabConnection(String name, String url, String apiTokenId, GitLabClientBuilder clientBuilder, boolean ignoreCertificateErrors, Integer connectionTimeout, Integer readTimeout) {
        this.name = name;
        this.url = url;
        this.apiTokenId = apiTokenId;
        this.clientBuilder = clientBuilder;
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

    public String getClientBuilderId() {
        return clientBuilder.id();
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

    public GitLabClient getClient() {
        if (apiCache == null) {
            apiCache = clientBuilder.buildClient(url, getApiToken(apiTokenId), ignoreCertificateErrors, connectionTimeout, readTimeout);
        }

        return apiCache;
    }

    private String getApiToken(String apiTokenId) {
        StandardCredentials credentials = CredentialsMatchers.firstOrNull(
            lookupCredentials(StandardCredentials.class, (Item) null, ACL.SYSTEM, new ArrayList<DomainRequirement>()),
            CredentialsMatchers.withId(apiTokenId));
        if (credentials != null) {
            if (credentials instanceof GitLabApiToken) {
                return ((GitLabApiToken) credentials).getApiToken().getPlainText();
            }
            if (credentials instanceof StringCredentials) {
                return ((StringCredentials) credentials).getSecret().getPlainText();
            }
        }
        throw new IllegalStateException("No credentials found for credentialsId: " + apiTokenId);
    }


    protected GitLabConnection readResolve() {
        if (connectionTimeout == null || readTimeout == null) {
            return new GitLabConnection(name, url, apiTokenId, new AutodetectGitLabClientBuilder(), ignoreCertificateErrors, 10, 10);
        }
        if (clientBuilder == null) {
            return new GitLabConnection(name, url, apiTokenId, new AutodetectGitLabClientBuilder(), ignoreCertificateErrors, connectionTimeout, readTimeout);
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
