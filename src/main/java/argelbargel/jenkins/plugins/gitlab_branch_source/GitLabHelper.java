package argelbargel.jenkins.plugins.gitlab_branch_source;

import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPI;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPIException;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.dabsquared.gitlabjenkins.connection.GitLabApiToken;
import com.dabsquared.gitlabjenkins.connection.GitLabConnection;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import hudson.model.Item;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

public final class GitLabHelper {
    private GitLabHelper() { /* no instances allowed */ }

    public static GitLabAPI gitLabAPI(String connectionName) throws GitLabAPIException {
        GitLabConnection connection = gitLabConnection(connectionName);
        return GitLabAPI.connect(connection.getUrl(), gitLabApiToken(connection.getApiTokenId()));
    }

    public static GitLabConnection gitLabConnection(String connectionName) {
        for (GitLabConnection conn : connectionConfig().getConnections()) {
            if (conn.getName().equals(connectionName)) {
                return conn;
            }
        }

        throw new NoSuchElementException("unknown gitlab-connection: " + connectionName);
    }

    static Collection<String> gitLabConnectionNames() {
        GitLabConnectionConfig config = connectionConfig();
        Collection<String> names = new ArrayList<>();

        for (GitLabConnection conn : config.getConnections()) {
            names.add(conn.getName());
        }

        return names;
    }

    static String gitLabConnectionId(String connectionName) {
        try {
            GitLabConnection conn = gitLabConnection(connectionName);
            return DigestUtils.md5Hex(conn.getUrl()) + "::" + DigestUtils.md5Hex(gitLabApiToken(conn.getApiTokenId()));
        } catch (NoSuchElementException e) {
            return DigestUtils.md5Hex(connectionName);
        }
    }

    private static GitLabConnectionConfig connectionConfig() {
        return (GitLabConnectionConfig) Jenkins.getInstance().getDescriptor(GitLabConnectionConfig.class);
    }


    private static String gitLabApiToken(String id) {
        StandardCredentials credentials = CredentialsMatchers.firstOrNull(
                lookupCredentials(StandardCredentials.class, (Item) null, ACL.SYSTEM, new ArrayList<DomainRequirement>()),
                CredentialsMatchers.withId(id));
        if (credentials != null) {
            if (credentials instanceof GitLabApiToken) {
                return ((GitLabApiToken) credentials).getApiToken().getPlainText();
            }
        }

        throw new IllegalStateException("No gitlab-api-token found for id: " + id);
    }
}
