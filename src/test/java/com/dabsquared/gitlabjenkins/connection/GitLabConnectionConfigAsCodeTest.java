package com.dabsquared.gitlabjenkins.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainCredentials;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.Util;
import io.jenkins.plugins.casc.model.CNode;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;

public class GitLabConnectionConfigAsCodeTest {
    @Rule
    public RuleChain chain = RuleChain.outerRule(
                    new EnvironmentVariables().set("BIND_TOKEN", "qwertyuiopasdfghjklzxcvbnm"))
            .around(new JenkinsConfiguredWithCodeRule());

    @Test
    @ConfiguredWithCode("global-config.yml")
    public void configure_gitlab_api_token() throws Exception {
        SystemCredentialsProvider systemCreds = SystemCredentialsProvider.getInstance();
        List<DomainCredentials> domainCredentials = systemCreds.getDomainCredentials();
        assertEquals(1, domainCredentials.size());
        final DomainCredentials gitLabCredential = domainCredentials.get(0);
        assertEquals(Domain.global(), gitLabCredential.getDomain());
        assertEquals(1, gitLabCredential.getCredentials().size());
        final GitLabApiToken apiToken =
                (GitLabApiToken) gitLabCredential.getCredentials().get(0);
        assertEquals("gitlab_token", apiToken.getId());
        assertEquals("qwertyuiopasdfghjklzxcvbnm", apiToken.getApiToken().getPlainText());
        assertEquals("Gitlab Token", apiToken.getDescription());
    }

    @Test
    @ConfiguredWithCode("global-config.yml")
    public void configure_gitlab_connection() throws Exception {
        final Jenkins jenkins = Jenkins.get();
        final GitLabConnectionConfig gitLabConnections = jenkins.getDescriptorByType(GitLabConnectionConfig.class);
        assertEquals(1, gitLabConnections.getConnections().size());
        final GitLabConnection gitLabConnection =
                gitLabConnections.getConnections().get(0);
        assertEquals("gitlab_token", gitLabConnection.getApiTokenId());
        assertEquals("my_gitlab_server", gitLabConnection.getName());
        assertEquals("autodetect", gitLabConnection.getClientBuilderId());
        assertEquals("https://gitlab.com/", gitLabConnection.getUrl());
        assertEquals(60, gitLabConnection.getConnectionTimeout());
        assertEquals(60, gitLabConnection.getReadTimeout());
        assertTrue(gitLabConnection.isIgnoreCertificateErrors());
    }

    @Test
    @ConfiguredWithCode("global-config.yml")
    public void export_configuration() throws Exception {
        GitLabConnectionConfig globalConfiguration = GitLabConnectionConfig.get();

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final Configurator c = context.lookupOrFail(GitLabConnectionConfig.class);

        @SuppressWarnings("unchecked")
        CNode node = c.describe(globalConfiguration, context);
        assertNotNull(node);
        String exported = Util.toYamlString(node);
        String expected = Util.toStringFromYamlFile(this, "global-config-expected.yml");
        assertEquals(expected, exported);
    }
}
