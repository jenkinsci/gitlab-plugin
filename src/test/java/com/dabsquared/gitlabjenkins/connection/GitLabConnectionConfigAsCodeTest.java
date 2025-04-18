package com.dabsquared.gitlabjenkins.connection;

import static org.junit.jupiter.api.Assertions.*;

import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainCredentials;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.Util;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import io.jenkins.plugins.casc.model.CNode;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@WithJenkinsConfiguredWithCode
@SetEnvironmentVariable(key = "BIND_TOKEN", value = "qwertyuiopasdfghjklzxcvbnm")
class GitLabConnectionConfigAsCodeTest {

    @Test
    @ConfiguredWithCode("global-config.yml")
    void configure_gitlab_api_token(JenkinsConfiguredWithCodeRule r) {
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
        assertEquals("GitLab Token", apiToken.getDescription());
    }

    @Test
    @ConfiguredWithCode("global-config.yml")
    void configure_gitlab_connection(JenkinsConfiguredWithCodeRule r) {
        final Jenkins jenkins = Jenkins.get();
        final GitLabConnectionConfig gitLabConnections = jenkins.getDescriptorByType(GitLabConnectionConfig.class);
        assertEquals(1, gitLabConnections.getConnections().size());
        final GitLabConnection gitLabConnection =
                gitLabConnections.getConnections().get(0);
        assertEquals("gitlab_token", gitLabConnection.getApiTokenId());
        assertEquals("my_gitlab_server", gitLabConnection.getName());
        assertEquals("autodetect", gitLabConnection.getClientBuilderId());
        assertEquals("https://gitlab.com/", gitLabConnection.getUrl());
        assertEquals(20, gitLabConnection.getConnectionTimeout());
        assertEquals(10, gitLabConnection.getReadTimeout());
        assertTrue(gitLabConnection.isIgnoreCertificateErrors());
    }

    @Test
    @ConfiguredWithCode("global-config.yml")
    void export_configuration(JenkinsConfiguredWithCodeRule r) throws Exception {
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
