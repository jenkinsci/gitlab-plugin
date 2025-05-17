package com.dabsquared.gitlabjenkins.connection;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Item;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * @author Robin Müller
 */
@Extension
public class GitLabConnectionConfig extends GlobalConfiguration {

    private Boolean useAuthenticatedEndpoint = true;
    private List<GitLabConnection> connections = new ArrayList<>();
    private transient Map<String, GitLabConnection> connectionMap = new HashMap<>();

    @DataBoundConstructor
    public GitLabConnectionConfig() {
        load();
        refreshConnectionMap();
    }

    public boolean isUseAuthenticatedEndpoint() {
        return useAuthenticatedEndpoint;
    }

    @DataBoundSetter
    public void setUseAuthenticatedEndpoint(boolean useAuthenticatedEndpoint) {
        this.useAuthenticatedEndpoint = useAuthenticatedEndpoint;
        save();
    }

    public List<GitLabConnection> getConnections() {
        return connections;
    }

    private void addConnection(
            List<GitLabConnection> list, Map<String, GitLabConnection> map, GitLabConnection connection) {
        String name = connection.getName();
        if (map.containsKey(name)) {
            throw new IllegalArgumentException(Messages.name_exists(name));
        }
        list.add(connection);
        map.put(name, connection);
    }

    public void addConnection(GitLabConnection connection) {
        addConnection(connections, connectionMap, connection);
    }

    @DataBoundSetter
    public void setConnections(List<GitLabConnection> newConnections) {
        List<GitLabConnection> tempConnections = new ArrayList<>();
        Map<String, GitLabConnection> tempConnectionMap = new HashMap<>();

        for (GitLabConnection connection : newConnections) {
            addConnection(tempConnections, tempConnectionMap, connection);
        }

        connections = tempConnections;
        connectionMap = tempConnectionMap;
        save();
    }

    public GitLabClient getClient(String connectionName, Item item, String jobCredentialId) {
        if (!connectionMap.containsKey(connectionName)) {
            return null;
        }
        return connectionMap.get(connectionName).getClient(item, jobCredentialId);
    }

    private void refreshConnectionMap() {
        connectionMap.clear();
        for (GitLabConnection connection : connections) {
            connectionMap.put(connection.getName(), connection);
        }
    }

    // For backwards compatibility. ReadResolve is called on startup
    protected GitLabConnectionConfig readResolve() {
        if (useAuthenticatedEndpoint == null) {
            setUseAuthenticatedEndpoint(false);
        }
        return this;
    }

    public static GitLabConnectionConfig get() {
        return ExtensionList.lookupSingleton(GitLabConnectionConfig.class);
    }

    @Override
    public boolean configure(StaplerRequest2 req, JSONObject json) throws FormException {
        try {
            return super.configure(req, json);
        } catch (IllegalArgumentException e) {
            throw new FormException(e.getMessage(), "connections");
        }
    }
}
