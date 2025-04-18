package com.dabsquared.gitlabjenkins.connection;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.dabsquared.gitlabjenkins.connection.GitLabConnection.DescriptorImpl;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.util.List;
import jenkins.model.Jenkins;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockserver.socket.PortFactory;

/**
 * @author Robin Müller
 */
@WithJenkins
class GitLabConnectionConfigSSLTest {

    private static final String API_TOKEN_ID = "apiTokenId";

    private static int port;

    private static Server server;

    private static JenkinsRule jenkins;

    @BeforeAll // based on
    // https://www.eclipse.org/jetty/documentation/9.4.x/embedded-examples.html#Multiple%20Connectors
    static void setUp(JenkinsRule rule) throws Exception {
        jenkins = rule;
        port = PortFactory.findFreePort();
        int _http_port = PortFactory.findFreePort();

        // Create a basic jetty server object without declaring the port. Since
        // we are configuring connectors directly we'll be setting ports on
        // those connectors.
        server = new Server();

        // HTTP Configuration
        // HttpConfiguration is a collection of configuration information
        // appropriate for http and https. The default scheme for http is
        // <code>http</code> of course, as the default for secured http is
        // <code>https</code> but we show setting the scheme to show it can be
        // done. The port for secured communication is also set here.
        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecureScheme("https");
        http_config.setSecurePort(port);
        http_config.setOutputBufferSize(32768);

        // HTTP connector
        // The first server connector we create is the one for http, passing in
        // the http configuration we configured above so it can get things like
        // the output buffer size, etc. We also set the port (8080) and
        // configure an idle timeout.
        ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
        http.setPort(_http_port);
        http.setIdleTimeout(30000);

        // SSL Context Factory for HTTPS
        // SSL requires a certificate so we configure a factory for ssl contents
        // with information pointing to what keystore the ssl connection needs
        // to know about. Much more configuration is available the ssl context,
        // including things like choosing the particular certificate out of a
        // keystore to be used.

        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStorePath("src/test/resources/keystore");
        sslContextFactory.setKeyStorePassword("password");

        // OPTIONAL: Un-comment the following to use Conscrypt for SSL instead of
        // the native JSSE implementation.

        // Security.addProvider(new OpenSSLProvider());
        // sslContextFactory.setProvider("Conscrypt");

        // HTTPS Configuration
        // A new HttpConfiguration object is needed for the next connector and
        // you can pass the old one as an argument to effectively clone the
        // contents. On this HttpConfiguration object we add a
        // SecureRequestCustomizer which is how a new connector is able to
        // resolve the https connection before handing control over to the Jetty
        // Server.
        HttpConfiguration https_config = new HttpConfiguration(http_config);
        SecureRequestCustomizer src = new SecureRequestCustomizer();
        src.setStsMaxAge(2000);
        src.setStsIncludeSubDomains(true);
        https_config.addCustomizer(src);

        // HTTPS connector
        // We create a second ServerConnector, passing in the http configuration
        // we just made along with the previously created ssl context factory.
        // Next we set the port and a longer idle timeout.
        ServerConnector https = new ServerConnector(
                server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(https_config));
        https.setPort(port);
        https.setIdleTimeout(500000);

        // Set the connectors
        server.setConnectors(new Connector[] {http, https});

        server.setHandler(new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) {
                response.setStatus(HttpStatus.OK_200);
                return true;
            }
        });
        server.start();
    }

    @AfterAll
    static void tearDown() throws Exception {
        server.stop();
    }

    @BeforeEach
    void setUp() throws Exception {
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstanceOrNull())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(
                        domains.get(0),
                        new StringCredentialsImpl(
                                CredentialsScope.SYSTEM,
                                API_TOKEN_ID,
                                "GitLab API Token",
                                Secret.fromString(API_TOKEN_ID)));
            }
        }
    }

    @Test
    void doCheckConnection_certificateError() {
        GitLabConnection.DescriptorImpl descriptor =
                (DescriptorImpl) jenkins.jenkins.getDescriptor(GitLabConnection.class);

        FormValidation formValidation =
                descriptor.doTestConnection("https://localhost:" + port + "/gitlab", API_TOKEN_ID, "v3", false, 10, 10);
        assertThat(formValidation.getMessage(), containsString(Messages.connection_error("PKIX path building failed")));
    }
}
