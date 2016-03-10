package com.dabsquared.gitlabjenkins.connection;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
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
    private transient Map<String, GitlabAPI> clients = new HashMap<String, GitlabAPI>();

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

    public GitlabAPI getClient(String connectionName) {
        if (!clients.containsKey(connectionName) && connectionMap.containsKey(connectionName)) {
            GitLabConnection connection = connectionMap.get(connectionName);
            GitlabAPI client = GitlabAPI.connect(connection.getUrl(), connection.getApiToken());
            client.ignoreCertificateErrors(connection.isIgnoreCertificateErrors());
            clients.put(connectionName, client);
        }
        return clients.get(connectionName);
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

    public FormValidation doTestConnection(@QueryParameter String url, @QueryParameter String apiToken, @QueryParameter boolean ignoreCertificateErrors) throws IOException {
        try {
            checkConnection(apiToken, url, ignoreCertificateErrors);
            return FormValidation.ok(Messages.connection_success());
        } catch (IOException e) {
            return FormValidation.error(Messages.connection_error(e.getMessage()));
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
