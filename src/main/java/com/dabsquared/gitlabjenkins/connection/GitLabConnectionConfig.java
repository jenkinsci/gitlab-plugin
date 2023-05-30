package com.dabsquared.gitlabjenkins.connection;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Item;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jenkins.model.GlobalConfiguration;
import org.gitlab4j.api.GitLabApi;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

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

    public void addConnection(GitLabConnection connection) {
        connections.add(connection);
        connectionMap.put(connection.getName(), connection);
    }

    @DataBoundSetter
    public void setConnections(List<GitLabConnection> newConnections) {
        connections = new ArrayList<>();
        connectionMap = new HashMap<>();
        for (GitLabConnection connection : newConnections) {
            addConnection(connection);
        }
        save();
    }

    public GitLabApi getGitLabApi(String connectionName, Item item, String jobCredentialId) {
        if (!connectionMap.containsKey(connectionName)) {
            return null;
        }
        return connectionMap.get(connectionName).getGitLabApi(item, jobCredentialId);
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
}
