package com.dabsquared.gitlabjenkins.connection;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.gitlab.GitLabClientBuilder;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractProject;
import hudson.triggers.Trigger;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.gitlab.api.GitlabAPI;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Robin Müller
 */
@Extension
public class GitLabConnectionConfig extends GlobalConfiguration {

    private List<GitLabConnection> connections = new ArrayList<GitLabConnection>();
    private transient Map<String, GitLabConnection> connectionMap = new HashMap<String, GitLabConnection>();
    private transient Map<String, GitLabApi> clients = new HashMap<String, GitLabApi>();

    public GitLabConnectionConfig() {
        load();
        refreshConnectionMap();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        connections = req.bindJSONToList(GitLabConnection.class, json.get("connections"));
        refreshConnectionMap();
        save();
        return super.configure(req, json);
    }

    public List<GitLabConnection> getConnections() {
        return connections;
    }

    public GitLabApi getClient(String connectionName) {
        if (!clients.containsKey(connectionName) && connectionMap.containsKey(connectionName)) {
            clients.put(connectionName, GitLabClientBuilder.buildClient(connectionMap.get(connectionName)));
        }
        return clients.get(connectionName);
    }

    @Deprecated
    public GitlabAPI getOldClient(String connectionName) {
        GitLabConnection connection = connectionMap.get(connectionName);
        GitlabAPI client = GitlabAPI.connect(connection.getUrl(), connection.getApiToken());
        client.ignoreCertificateErrors(connection.isIgnoreCertificateErrors());
        return client;
    }

    public FormValidation doCheckName(@QueryParameter String id, @QueryParameter String value) {
        if (value == null || value.isEmpty()) {
            return FormValidation.error(Messages.name_required());
        } else if (connectionMap.containsKey(value) && !connectionMap.get(value).toString().equals(id)) {
            return FormValidation.error(Messages.name_exists(value));
        } else {
            return FormValidation.ok();
        }
    }

    public FormValidation doCheckUrl(@QueryParameter String value) {
        if (value == null || value.isEmpty()) {
            return FormValidation.error(Messages.url_required());
        } else {
            return FormValidation.ok();
        }
    }

    public FormValidation doCheckApiToken(@QueryParameter String value) {
        if (value == null || value.isEmpty()) {
            return FormValidation.error(Messages.apiToken_required());
        } else {
            return FormValidation.ok();
        }
    }

    public FormValidation doTestConnection(@QueryParameter String url, @QueryParameter String apiToken, @QueryParameter boolean ignoreCertificateErrors) {
        try {
            GitLabClientBuilder.buildClient(url, apiToken, ignoreCertificateErrors).headCurrentUser();
            return FormValidation.ok(Messages.connection_success());
        } catch (WebApplicationException e) {
            return FormValidation.error(Messages.connection_error(e.getMessage()));
        } catch (ProcessingException e) {
            return FormValidation.error(Messages.connection_error(e.getCause().getMessage()));
        }
    }

    private void refreshConnectionMap() {
        connectionMap.clear();
        for (GitLabConnection connection : connections) {
            connectionMap.put(connection.getName(), connection);
        }
    }

    public static boolean checkConnection(String token, String url, boolean ignoreCertificateErrors) throws IOException {
        GitlabAPI testApi = GitlabAPI.connect(url, token);
        testApi.ignoreCertificateErrors(ignoreCertificateErrors);
        testApi.getProjects();
        return true;
    }
}
