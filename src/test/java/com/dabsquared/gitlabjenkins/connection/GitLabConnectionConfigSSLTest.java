package com.dabsquared.gitlabjenkins.connection;


import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.socket.PortFactory;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.security.SslSocketConnector;

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
        SslSocketConnector sslSocketConnector = new SslSocketConnector();
        sslSocketConnector.setKeystore("src/test/resources/keystore");
        sslSocketConnector.setKeyPassword("password");
        sslSocketConnector.setPort(port);
        server.setConnectors(new Connector[]{sslSocketConnector});
        server.addHandler(new AbstractHandler() {
            public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
                response.setStatus(Response.Status.OK.getStatusCode());
                Request base_request = request instanceof Request ? (Request) request : HttpConnection.getCurrentConnection().getRequest();
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
