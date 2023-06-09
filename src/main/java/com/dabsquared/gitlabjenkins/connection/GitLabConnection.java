package com.dabsquared.gitlabjenkins.connection;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder.getAllGitLabClientBuilders;
import static com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder.getGitLabClientBuilderById;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import com.dabsquared.gitlabjenkins.gitlab.api.impl.AutodetectGitLabClientBuilder;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.WebApplicationException;
import jenkins.model.Jenkins;
import org.eclipse.jgit.util.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * @author Robin Müller
 */
public class GitLabConnection extends AbstractDescribableImpl<GitLabConnection> {
    private final String name;
    private final String url;
    private transient String apiToken;
    // TODO make final when migration code gets removed
    private String apiTokenId;
    private GitLabClientBuilder clientBuilder;
    private final boolean ignoreCertificateErrors;
    private final Integer connectionTimeout;
    private final Integer readTimeout;
    private transient Map<String, GitLabApi> clientCache;

    public GitLabConnection(
            String name,
            String url,
            String apiTokenId,
            boolean ignoreCertificateErrors,
            Integer connectionTimeout,
            Integer readTimeout) {
        this(
                name,
                url,
                apiTokenId,
                new AutodetectGitLabClientBuilder(),
                ignoreCertificateErrors,
                connectionTimeout,
                readTimeout);
    }

    @DataBoundConstructor
    public GitLabConnection(
            String name,
            String url,
            String apiTokenId,
            String clientBuilderId,
            boolean ignoreCertificateErrors,
            Integer connectionTimeout,
            Integer readTimeout) {
        this(
                name,
                url,
                apiTokenId,
                getGitLabClientBuilderById(clientBuilderId),
                ignoreCertificateErrors,
                connectionTimeout,
                readTimeout);
    }

    @Restricted(NoExternalUse.class)
    public GitLabConnection(
            String name,
            String url,
            String apiTokenId,
            GitLabClientBuilder clientBuilder,
            boolean ignoreCertificateErrors,
            Integer connectionTimeout,
            Integer readTimeout) {
        this.name = name;
        this.url = url == null ? "" : url;
        this.apiTokenId = apiTokenId;
        this.clientBuilder = clientBuilder;
        this.ignoreCertificateErrors = ignoreCertificateErrors;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        clientCache = new HashMap<>();
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

    public GitLabApi getClient(Item item, String jobCredentialId) {
        final String clientId;
        final String token;
        if ((jobCredentialId == null) || jobCredentialId.equals(apiTokenId)) {
            clientId = "global";
            token = getApiToken(apiTokenId, null);
        } else {
            // Add prefix to credential ID to avoid collision with "global"
            clientId = "alternative-" + jobCredentialId;
            token = getApiToken(jobCredentialId, item);
        }

        if (!clientCache.containsKey(clientId)) {
            clientCache.put(
                    clientId,
                    clientBuilder.buildClient(url, token, ignoreCertificateErrors, connectionTimeout, readTimeout));
        }
        return clientCache.get(clientId);
    }

    @Restricted(NoExternalUse.class)
    private String getApiToken(String apiTokenId, Item item) {
        ItemGroup<?> context = item != null ? item.getParent() : Jenkins.get();
        StandardCredentials credentials = CredentialsMatchers.firstOrNull(
                lookupCredentials(
                        StandardCredentials.class,
                        context,
                        ACL.SYSTEM,
                        URIRequirementBuilder.fromUri(url).build()),
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
            return new GitLabConnection(
                    name, url, apiTokenId, new AutodetectGitLabClientBuilder(), ignoreCertificateErrors, 10, 10);
        }
        if (clientBuilder == null) {
            return new GitLabConnection(
                    name,
                    url,
                    apiTokenId,
                    new AutodetectGitLabClientBuilder(),
                    ignoreCertificateErrors,
                    connectionTimeout,
                    readTimeout);
        }
        if (clientCache == null) {
            clientCache = new HashMap<>();
        }

        return this;
    }

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    public static void migrate() throws IOException {
        GitLabConnectionConfig descriptor =
                (GitLabConnectionConfig) Jenkins.get().getDescriptor(GitLabConnectionConfig.class);
        if (descriptor == null) return;
        for (GitLabConnection connection : descriptor.getConnections()) {
            if (connection.apiTokenId == null && connection.apiToken != null) {
                for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstance())) {
                    if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                        List<Domain> domains = credentialsStore.getDomains();
                        connection.apiTokenId = UUID.randomUUID().toString();
                        credentialsStore.addCredentials(
                                domains.get(0),
                                new GitLabApiTokenImpl(
                                        CredentialsScope.SYSTEM,
                                        connection.apiTokenId,
                                        "GitLab API Token",
                                        Secret.fromString(connection.apiToken)));
                    }
                }
            }
        }
        descriptor.save();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<GitLabConnection> {
        public FormValidation doCheckName(@QueryParameter String id, @QueryParameter String value) {
            if (StringUtils.isEmptyOrNull(value)) {
                return FormValidation.error(Messages.name_required());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckUrl(@QueryParameter String value) {
            if (StringUtils.isEmptyOrNull(value)) {
                return FormValidation.error(Messages.url_required());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckApiTokenId(@QueryParameter String value) {
            if (StringUtils.isEmptyOrNull(value)) {
                return FormValidation.error(Messages.apiToken_required());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckConnectionTimeout(@QueryParameter Integer value) {
            if (value == null) {
                return FormValidation.error(Messages.connectionTimeout_required());
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckReadTimeout(@QueryParameter Integer value) {
            if (value == null) {
                return FormValidation.error(Messages.readTimeout_required());
            } else {
                return FormValidation.ok();
            }
        }

        @RequirePOST
        @Restricted(DoNotUse.class) // WebOnly
        public FormValidation doTestConnection(
                @QueryParameter String url,
                @QueryParameter String apiTokenId,
                @QueryParameter String clientBuilderId,
                @QueryParameter boolean ignoreCertificateErrors,
                @QueryParameter int connectionTimeout,
                @QueryParameter int readTimeout) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            try {
                new GitLabConnection(
                                "",
                                url,
                                apiTokenId,
                                clientBuilderId,
                                ignoreCertificateErrors,
                                connectionTimeout,
                                readTimeout)
                        .getClient(null, null)
                        .getUserApi()
                        .getCurrentUser();
                return FormValidation.ok(Messages.connection_success());
            } catch (WebApplicationException e) {
                return FormValidation.error(Messages.connection_error(e.getMessage()));
            } catch (GitLabApiException e) {
                return FormValidation.error(Messages.connection_error(e.getMessage()));
            }
        }

        public ListBoxModel doFillApiTokenIdItems(@QueryParameter String url, @QueryParameter String apiTokenId) {
            if (Jenkins.get().hasPermission(Item.CONFIGURE)) {
                return new StandardListBoxModel()
                        .includeEmptyValue()
                        .includeMatchingAs(
                                ACL.SYSTEM,
                                Jenkins.get(),
                                StandardCredentials.class,
                                URIRequirementBuilder.fromUri(url).build(),
                                new GitLabCredentialMatcher())
                        .includeCurrentValue(apiTokenId);
            }
            return new StandardListBoxModel();
        }

        public ListBoxModel doFillClientBuilderIdItems() {
            ListBoxModel model = new ListBoxModel();
            for (GitLabClientBuilder builder : getAllGitLabClientBuilders()) {
                model.add(builder.id());
            }

            return model;
        }
    }
}
