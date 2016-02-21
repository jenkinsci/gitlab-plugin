package com.dabsquared.gitlabjenkins.gitlab;

import com.dabsquared.gitlabjenkins.connection.GitLabConnection;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import jenkins.model.Jenkins;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.RuntimeDelegate;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Robin Müller
 */
public class GitLabClientBuilder {

    private final static Logger LOGGER = Logger.getLogger(GitLabClientBuilder.class.getName());

    public static GitLabApi buildClient(String gitlabHostUrl, final String gitlabApiToken, boolean ignoreCertificateErrors) {
        return new ResteasyClientBuilder()
                .httpEngine(new ApacheHttpClient4Engine(createHttpClient(ignoreCertificateErrors)))
                .register(new JacksonJsonProvider())
                .register(new JacksonConfig())
                .register(new ApiHeaderTokenFilter(gitlabApiToken)).build().target(gitlabHostUrl)
                .proxyBuilder(GitLabApi.class)
                .classloader(Jenkins.getInstance().getPluginManager().uberClassLoader)
                .build();
    }

    public static GitLabApi buildClient(GitLabConnection connection) {
        return buildClient(connection.getUrl(), connection.getApiToken(), connection.isIgnoreCertificateErrors());
    }

    @Initializer(before = InitMilestone.PLUGINS_STARTED)
    public static void setRuntimeDelegate() {
        RuntimeDelegate.setInstance(new ResteasyProviderFactory());
    }

    private static DefaultHttpClient createHttpClient(boolean ignoreCertificateErrors) {
        ClientConnectionManager connectionManager;
        if (ignoreCertificateErrors) {
            connectionManager = new BasicClientConnectionManager(createSchemeRegistry());
        } else {
            connectionManager = new BasicClientConnectionManager();
        }
        return new DefaultHttpClient(connectionManager, new DefaultHttpClient().getParams());
    }

    private static SchemeRegistry createSchemeRegistry() {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        try {
            SSLSocketFactory factory = new SSLSocketFactory(new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            registry.register(new Scheme("https", 10443, factory));
        } catch (GeneralSecurityException e) {
            LOGGER.log(Level.SEVERE, "Failed to set ignoreCertificateErrors", e);
        }
        return registry;
    }

    private static class ApiHeaderTokenFilter implements ClientRequestFilter {
        private final String gitlabApiToken;

        public ApiHeaderTokenFilter(String gitlabApiToken) {
            this.gitlabApiToken = gitlabApiToken;
        }

        public void filter(ClientRequestContext requestContext) throws IOException {
            requestContext.getHeaders().putSingle("PRIVATE-TOKEN", gitlabApiToken);
        }
    }
}
