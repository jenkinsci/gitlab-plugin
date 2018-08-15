package com.dabsquared.gitlabjenkins.connection;


import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.socket.PortFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Robin Müller
 */
public class GitLabConnectionConfigSSLTest {

    private static final String API_TOKEN_ID = "apiTokenId";

    private static int port;

    private static Server server;

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @BeforeClass
    public static void startJetty() throws Exception {
        port = PortFactory.findFreePort();
        server = new Server();

        HttpConfiguration https_config = new HttpConfiguration();
        https_config.setSecureScheme("https");
        https_config.setSecurePort(port);
        https_config.setOutputBufferSize(32768);
        ServerConnector https = new ServerConnector(server,new HttpConnectionFactory(https_config));
        https.setPort(port);
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath("src/test/resources/keystore");
        sslContextFactory.setKeyStorePassword("password");
        server.setConnectors(new Connector[]{https});
        HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.setHandlers(new Handler[] {
            new AbstractHandler() {
                public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                }
            }
        });
        server.setHandler(handlerCollection);
        server.start();
    }

    @AfterClass
    public static void stopJetty() throws Exception {
        server.stop();
    }

    @Before
    public void setup() throws IOException {
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstance())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(domains.get(0),
                    new StringCredentialsImpl(CredentialsScope.SYSTEM, API_TOKEN_ID, "GitLab API Token", Secret.fromString(API_TOKEN_ID)));
            }
        }
    }

    @Test
    public void doCheckConnection_ignoreCertificateErrors() {
        GitLabConnectionConfig connectionConfig = jenkins.get(GitLabConnectionConfig.class);

        FormValidation formValidation = connectionConfig.doTestConnection("https://localhost:" + port + "/gitlab", API_TOKEN_ID, "v3", true, 10, 10);
        assertThat(formValidation.getMessage(), is(Messages.connection_success()));
    }

    @Test
    public void doCheckConnection_certificateError() throws IOException {
        GitLabConnectionConfig connectionConfig = jenkins.get(GitLabConnectionConfig.class);

        FormValidation formValidation = connectionConfig.doTestConnection("https://localhost:" + port + "/gitlab", API_TOKEN_ID, "v3", false, 10, 10);
        assertThat(formValidation.getMessage(), containsString(Messages.connection_error("")));
    }
}
