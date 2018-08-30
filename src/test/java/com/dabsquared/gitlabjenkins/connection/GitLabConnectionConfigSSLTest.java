package com.dabsquared.gitlabjenkins.connection;


import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;

import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.socket.PortFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
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
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath("src/test/resources/keystore");
        sslContextFactory.setKeyStorePassword("password");
        ServerConnector https = new ServerConnector(server, sslContextFactory);
        https.setPort(port);
        server.setConnectors(new Connector[]{https});
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
                response.setStatus(Response.Status.OK.getStatusCode());
                Request base_request = (request instanceof Request) ? (Request)request:HttpConnection.getCurrentConnection().getHttpChannel().getRequest();
                base_request.setHandled(true);
            }
        });
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
