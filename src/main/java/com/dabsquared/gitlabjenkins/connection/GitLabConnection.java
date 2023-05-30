/* Note for Reviewers :
 * Proxy settings will be configured here, not implimented currently
 * feature of filling clientbuilderid by user and the respective constructors has been removed
 */

package com.dabsquared.gitlabjenkins.connection;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
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
import javax.ws.rs.ProcessingException;
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
    private GitLabApi gitLabApi;
    private final boolean ignoreCertificateErrors;
    private final Integer connectionTimeout;
    private final Integer readTimeout;
    private transient Map<String, GitLabApi> gitLabApiCache;

    @DataBoundConstructor
    public GitLabConnection(
            String name,
            String url,
            String apiTokenId,
            boolean ignoreCertificateErrors,
            Integer connectionTimeout,
            Integer readTimeout) {
        this.name = name;
        this.url = url == null ? "" : url;
        this.apiTokenId = apiTokenId;
        this.ignoreCertificateErrors = ignoreCertificateErrors;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        createGitLabApi(url, apiToken);
        gitLabApiCache = new HashMap<>();
    }

    private GitLabApi createGitLabApi(String url, String apiToken) {
        return gitLabApi = new GitLabApi(url, apiToken);
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

    public GitLabApi getGitLabApi(Item item, String jobCredentialId) {
        final String gitLabApiId;
        final String token;
        if ((jobCredentialId == null) || jobCredentialId.equals(apiTokenId)) {
            gitLabApiId = "global";
            token = getApiToken(apiTokenId, null);
        } else {
            // Add prefix to credential ID to avoid collision with "global"
            gitLabApiId = "alternative-" + jobCredentialId;
            token = getApiToken(jobCredentialId, item);
        }

        if (!gitLabApiCache.containsKey(gitLabApiId)) {
            gitLabApiCache.put(gitLabApiId, createGitLabApi(url, token));
        }
        return gitLabApiCache.get(gitLabApiId);

        // proxy configuration setup has to be done here
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
            return new GitLabConnection(name, url, apiTokenId, ignoreCertificateErrors, 10, 10);
        }
        if (gitLabApi == null) {
            return new GitLabConnection(name, url, apiTokenId, ignoreCertificateErrors, connectionTimeout, readTimeout);
        }
        if (gitLabApiCache == null) {
            gitLabApiCache = new HashMap<>();
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
                @QueryParameter boolean ignoreCertificateErrors,
                @QueryParameter int connectionTimeout,
                @QueryParameter int readTimeout) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            try {
                new GitLabConnection("", url, apiTokenId, ignoreCertificateErrors, connectionTimeout, readTimeout)
                        .getGitLabApi(null, null)
                        .getUserApi()
                        .getCurrentUser();
                return FormValidation.ok(Messages.connection_success());
            } catch (WebApplicationException e) {
                return FormValidation.error(Messages.connection_error(e.getMessage()));
            } catch (ProcessingException e) {
                return FormValidation.error(
                        Messages.connection_error(e.getCause().getMessage()));
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
    }
}
