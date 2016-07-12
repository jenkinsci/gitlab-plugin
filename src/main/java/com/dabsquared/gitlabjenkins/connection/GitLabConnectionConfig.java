package com.dabsquared.gitlabjenkins.connection;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.common.AbstractIdCredentialsListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.dabsquared.gitlabjenkins.gitlab.GitLabClientBuilder;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.eclipse.jgit.util.StringUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Robin Müller
 */
@Extension
public class GitLabConnectionConfig extends GlobalConfiguration {

    private boolean useAuthenticatedEndpoint;
    private List<GitLabConnection> connections = new ArrayList<>();
    private transient Map<String, GitLabConnection> connectionMap = new HashMap<>();
    private transient Map<String, GitLabApi> clients = new HashMap<>();

    public GitLabConnectionConfig() {
        load();
        refreshConnectionMap();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        connections = req.bindJSONToList(GitLabConnection.class, json.get("connections"));
        useAuthenticatedEndpoint = json.getBoolean("useAuthenticatedEndpoint");
        refreshConnectionMap();
        clients.clear();
        save();
        return super.configure(req, json);
    }

    public boolean isUseAuthenticatedEndpoint() {
        return useAuthenticatedEndpoint;
    }

    void setUseAuthenticatedEndpoint(boolean useAuthenticatedEndpoint) {
        this.useAuthenticatedEndpoint = useAuthenticatedEndpoint;
    }

    public List<GitLabConnection> getConnections() {
        return connections;
    }

    public void addConnection(GitLabConnection connection) {
        connections.add(connection);
        connectionMap.put(connection.getName(), connection);
    }

    public GitLabApi getClient(String connectionName) {
        if (!clients.containsKey(connectionName) && connectionMap.containsKey(connectionName)) {
            clients.put(connectionName, GitLabClientBuilder.buildClient(connectionMap.get(connectionName)));
        }
        return clients.get(connectionName);
    }

    public FormValidation doCheckName(@QueryParameter String id, @QueryParameter String value) {
        if (StringUtils.isEmptyOrNull(value)) {
            return FormValidation.error(Messages.name_required());
        } else if (connectionMap.containsKey(value) && !connectionMap.get(value).toString().equals(id)) {
            return FormValidation.error(Messages.name_exists(value));
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

    public FormValidation doTestConnection(@QueryParameter String url,
                                           @QueryParameter String apiTokenId,
                                           @QueryParameter boolean ignoreCertificateErrors,
                                           @QueryParameter int connectionTimeout,
                                           @QueryParameter int readTimeout) {
        try {
            GitLabClientBuilder.buildClient(url, apiTokenId, ignoreCertificateErrors, connectionTimeout, readTimeout).headCurrentUser();
            return FormValidation.ok(Messages.connection_success());
        } catch (WebApplicationException e) {
            return FormValidation.error(Messages.connection_error(e.getMessage()));
        } catch (ProcessingException e) {
            return FormValidation.error(Messages.connection_error(e.getCause().getMessage()));
        }
    }

    public ListBoxModel doFillApiTokenIdItems(@QueryParameter String name, @QueryParameter String url) {
        if (Jenkins.getInstance().hasPermission(Item.CONFIGURE)) {
            AbstractIdCredentialsListBoxModel<StandardListBoxModel, StandardCredentials> options = new StandardListBoxModel()
                .includeEmptyValue()
                .includeMatchingAs(ACL.SYSTEM,
                                   Jenkins.getActiveInstance(),
                                   StandardCredentials.class,
                                   URIRequirementBuilder.fromUri(url).build(),
                                   new GitLabCredentialMatcher());
            if (name != null && connectionMap.containsKey(name)) {
                String apiTokenId = connectionMap.get(name).getApiTokenId();
                options.includeCurrentValue(apiTokenId);
                for (ListBoxModel.Option option : options) {
                    if (option.value.equals(apiTokenId)) {
                        option.selected = true;
                    }
                }
            }
            return options;
        }
        return new StandardListBoxModel();
    }

    private void refreshConnectionMap() {
        connectionMap.clear();
        for (GitLabConnection connection : connections) {
            connectionMap.put(connection.getName(), connection);
        }
    }

    private static class GitLabCredentialMatcher implements CredentialsMatcher {
        @Override
        public boolean matches(@NonNull Credentials credentials) {
            return credentials instanceof GitLabApiToken || credentials instanceof StringCredentials;
        }
    }
}
