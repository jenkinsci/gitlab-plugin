package com.dabsquared.gitlabjenkins.connection;

import hudson.util.FormValidation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Robin Müller
 */
public class GitLabConnectionConfigSSLTest {

    private static int port;
    private static Server server;

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

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

    @Test
    public void doCheckConnection_ignoreCertificateErrors() {
        String apiToken = "secret";
        GitLabConnectionConfig connectionConfig = jenkins.get(GitLabConnectionConfig.class);
        FormValidation formValidation = connectionConfig.doTestConnection("https://localhost:" + port + "/gitlab", apiToken, true);
        assertThat(formValidation.getMessage(), is(Messages.connection_success()));
    }

    @Test
    public void doCheckConnection_certificateError() {
        String apiToken = "secret";
        GitLabConnectionConfig connectionConfig = jenkins.get(GitLabConnectionConfig.class);
        FormValidation formValidation = connectionConfig.doTestConnection("https://localhost:" + port + "/gitlab", apiToken, false);
        assertThat(formValidation.getMessage(), is(Messages.connection_error("peer not authenticated")));
    }
}
