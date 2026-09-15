package com.dabsquared.gitlabjenkins.connection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import com.dabsquared.gitlabjenkins.gitlab.api.impl.V3GitLabClientBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.security.Permission;
import hudson.util.Secret;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mockito;

@WithJenkins
class GitLabConnectionTest {
    private static final String API_TOKEN = "secret";
    private static final String API_TOKEN_ID = "apiTokenId";
    private static final String API_TOKEN_ID_2 = "apiTokenId2";
    private static final String ALT_CREDENTIAL_ID = "altCredentialId";

    private static JenkinsRule jenkins;

    private static GitLabConnection connection;

    @BeforeAll
    static void setUp(JenkinsRule rule) throws Exception {
        jenkins = rule;
        for (final CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.get())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                final List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(
                        domains.get(0),
                        new StringCredentialsImpl(
                                CredentialsScope.SYSTEM,
                                API_TOKEN_ID,
                                "GitLab API Token",
                                Secret.fromString(API_TOKEN)));
                credentialsStore.addCredentials(
                        domains.get(0),
                        new StringCredentialsImpl(
                                CredentialsScope.SYSTEM,
                                API_TOKEN_ID_2,
                                "GitLab API Token 2",
                                Secret.fromString(API_TOKEN)));
            }
        }

        connection = new GitLabConnection(
                "test", "http://localhost", API_TOKEN_ID, new V3GitLabClientBuilder(), false, 10, 10);
    }

    @Test
    void doFillApiTokenIdItemsTestAdmin() throws Exception {
        String response = retrieveResponseForUserFromFillApiTokenIdItems("admin", Jenkins.ADMINISTER);
        assertThat(response, containsString("gitlab-1"));
    }

    @Test
    @Issue("SECURITY-3260")
    void doFillApiTokenIdItemsTestConfigure() throws Exception {
        String response = retrieveResponseForUserFromFillApiTokenIdItems("config", Jenkins.READ, Item.CONFIGURE);
        assertThat(response, containsString("\"values\":[]"));
    }

    @Test
    void doFillApiTokenIdItemsTestRead() throws Exception {
        String response = retrieveResponseForUserFromFillApiTokenIdItems("dev", Jenkins.READ);
        assertThat(response, not(containsString("gitlab-1")));
    }

    private String retrieveResponseForUserFromFillApiTokenIdItems(String userName, Permission... permissionList)
            throws Exception {
        jenkins.jenkins.setSecurityRealm(jenkins.createDummySecurityRealm());
        jenkins.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(permissionList)
                .everywhere()
                .to(userName));

        StringCredentialsImpl gitLabToken = new StringCredentialsImpl(
                CredentialsScope.GLOBAL, "gitlab-1", "token", Secret.fromString("gitlab-token"));
        SystemCredentialsProvider.getInstance().getCredentials().add(gitLabToken);

        try (JenkinsRule.WebClient webClient = jenkins.createWebClient()) {
            webClient.login(userName);
            return webClient
                    .goTo(
                            "descriptorByName/com.dabsquared.gitlabjenkins.connection.GitLabConnection/fillApiTokenIdItems?url=http://xyz.com",
                            "application/json")
                    .getWebResponse()
                    .getContentAsString();
        }
    }

    @Test
    void getClient_nullCredentialId_sameClient() {
        final GitLabClient client = connection.getClient(null, null);
        assertThat(client, notNullValue());
        assertThat(connection.getClient(null, null), sameInstance(client));
    }

    @Test
    void getClient_nullAndDefaultCredentialId_sameClient() {
        final GitLabClient client = connection.getClient(null, null);
        assertThat(client, notNullValue());
        assertThat(connection.getClient(null, API_TOKEN_ID), sameInstance(client));
    }

    @Test
    void getClient_differentCredentialId_differentClient() {
        final GitLabClient client1 = connection.getClient(null, API_TOKEN_ID);
        assertThat(client1, notNullValue());
        final GitLabClient client2 = connection.getClient(null, API_TOKEN_ID_2);
        assertThat(client2, notNullValue());
        assertThat(client2, not((client1)));
    }

    @Test
    public void getClient_sameCredentialId_differentFolderContext_differentClient() {
        ItemGroup<?> folderA = mockItemGroup("folder-a");
        ItemGroup<?> folderB = mockItemGroup("folder-b");
        Item itemA = mockItem(folderA);
        Item itemB = mockItem(folderB);

        CountingGitLabClientBuilder builder = new CountingGitLabClientBuilder();
        GitLabConnection scopedConnection =
                new GitLabConnection("scoped", "http://localhost", API_TOKEN_ID, builder, false, 10, 10);

        GitLabClient clientA = scopedConnection.getClient(itemA, ALT_CREDENTIAL_ID);
        GitLabClient clientB = scopedConnection.getClient(itemB, ALT_CREDENTIAL_ID);

        assertThat(clientB, not(sameInstance(clientA)));
        assertThat(builder.getBuildCount(), is(2));
    }

    @Test
    public void getClient_sameCredentialId_sameFolderContext_sameClient() {
        ItemGroup<?> folder = mockItemGroup("folder-a");
        Item itemA = mockItem(folder);
        Item itemB = mockItem(folder);

        CountingGitLabClientBuilder builder = new CountingGitLabClientBuilder();
        GitLabConnection scopedConnection =
                new GitLabConnection("scoped", "http://localhost", API_TOKEN_ID, builder, false, 10, 10);

        GitLabClient clientA = scopedConnection.getClient(itemA, ALT_CREDENTIAL_ID);
        GitLabClient clientB = scopedConnection.getClient(itemB, ALT_CREDENTIAL_ID);

        assertThat(clientB, sameInstance(clientA));
        assertThat(builder.getBuildCount(), is(1));
    }

    private static ItemGroup<?> mockItemGroup(String fullName) {
        ItemGroup<?> folder = Mockito.mock(ItemGroup.class);
        Mockito.when(folder.getFullName()).thenReturn(fullName);
        return folder;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Item mockItem(ItemGroup<?> parent) {
        Item item = Mockito.mock(Item.class);
        Mockito.doReturn(parent).when(item).getParent();
        return item;
    }

    private static final class CountingGitLabClientBuilder extends GitLabClientBuilder {
        private final AtomicInteger buildCount = new AtomicInteger();
        private int clientCounter;

        CountingGitLabClientBuilder() {
            super("counting", 0);
        }

        @NonNull
        @Override
        public GitLabClient buildClient(
                String url,
                GitlabCredentialResolver credentialResolver,
                boolean ignoreCertificateErrors,
                int connectionTimeout,
                int readTimeout) {
            buildCount.incrementAndGet();
            return Mockito.mock(GitLabClient.class, "client-" + clientCounter++);
        }

        int getBuildCount() {
            return buildCount.get();
        }
    }
}
