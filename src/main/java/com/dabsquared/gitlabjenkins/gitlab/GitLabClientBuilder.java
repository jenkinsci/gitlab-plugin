package com.dabsquared.gitlabjenkins.gitlab;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.dabsquared.gitlabjenkins.connection.GitLabConnection;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.util.JsonUtil;
import com.dabsquared.gitlabjenkins.util.LoggerUtil;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Item;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import javax.annotation.Nullable;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.RuntimeDelegate;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

/**
 * @author Robin Müller
 */
public class GitLabClientBuilder {

    private final static Logger LOGGER = Logger.getLogger(GitLabClientBuilder.class.getName());
    private static final String PRIVATE_TOKEN = "PRIVATE-TOKEN";

    public static GitLabApi buildClient(String gitlabHostUrl, final String gitlabApiTokenId, boolean ignoreCertificateErrors) {
        return new ResteasyClientBuilder()
                .httpEngine(new ApacheHttpClient4Engine(createHttpClient(ignoreCertificateErrors)))
                .register(new JacksonJsonProvider())
                .register(new JacksonConfig())
                .register(new ApiHeaderTokenFilter(getApiToken(gitlabApiTokenId))).build().target(gitlabHostUrl)
                .register(new LoggingFilter())
                .proxyBuilder(GitLabApi.class)
                .classloader(Jenkins.getInstance().getPluginManager().uberClassLoader)
                .build();
    }

    public static GitLabApi buildClient(GitLabConnection connection) {
        return buildClient(connection.getUrl(), connection.getApiTokenId(), connection.isIgnoreCertificateErrors());
    }

    @Initializer(before = InitMilestone.PLUGINS_STARTED)
    public static void setRuntimeDelegate() {
        RuntimeDelegate.setInstance(new ResteasyProviderFactory());
    }

    private static String getApiToken(String apiTokenId) {
        StringCredentials credentials = CredentialsMatchers.firstOrNull(
            lookupCredentials(StringCredentials.class, (Item) null, ACL.SYSTEM, new ArrayList<DomainRequirement>()),
            CredentialsMatchers.withId(apiTokenId));
        return credentials == null ? null : credentials.getSecret().getPlainText();
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

        ApiHeaderTokenFilter(String gitlabApiToken) {
            this.gitlabApiToken = gitlabApiToken;
        }

        public void filter(ClientRequestContext requestContext) throws IOException {
            requestContext.getHeaders().putSingle(PRIVATE_TOKEN, gitlabApiToken);
        }
    }

    private static class LoggingFilter implements ClientRequestFilter, ClientResponseFilter {
        @Override
        public void filter(ClientRequestContext context) throws IOException {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Call GitLab:\nHTTP method: {0}\nURL: {1}\nRequest headers: [\n{2}\n]",
                        LoggerUtil.toArray(context.getMethod(), context.getUri(), toFilteredString(context.getHeaders())));
            }
        }

        @Override
        public void filter(ClientRequestContext request, ClientResponseContext response) throws IOException {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Got response from GitLab:\nURL: {0}\nStatus: {1} {2}\nResponse headers: [\n{3}\n]\nResponse body: {4}",
                        LoggerUtil.toArray(request.getUri(), response.getStatus(), response.getStatusInfo(), toString(response.getHeaders()),
                                getPrettyPrintResponseBody(response)));
            }
        }

        private String toFilteredString(MultivaluedMap<String, Object> headers) {
            return FluentIterable.from(headers.entrySet()).transform(new HeaderToFilteredString()).join(Joiner.on(",\n"));
        }

        private String toString(MultivaluedMap<String, String> headers) {
            return FluentIterable.from(headers.entrySet()).transform(new HeaderToString()).join(Joiner.on(",\n"));
        }

        private String getPrettyPrintResponseBody(ClientResponseContext responseContext) {
            String responseBody = getResponseBody(responseContext);
            if (StringUtils.isNotEmpty(responseBody) && responseContext.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)) {
                return JsonUtil.toPrettyPrint(responseBody);
            }
            return responseBody;
        }

        private String getResponseBody(ClientResponseContext context) {
            try (InputStream entityStream = context.getEntityStream()) {
                if (entityStream != null) {
                    byte[] bytes = IOUtils.toByteArray(entityStream);
                    context.setEntityStream(new ByteArrayInputStream(bytes));
                    return new String(bytes);
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failure during reading the response body", e);
                context.setEntityStream(new ByteArrayInputStream(new byte[0]));
            }
            return "";
        }

        private static class HeaderToFilteredString implements Function<Map.Entry<String, List<Object>>, String> {
            @Nullable
            @Override
            public String apply(@Nullable Map.Entry<String, List<Object>> input) {
                if (input == null) {
                    return null;
                } else if (input.getKey().equals(PRIVATE_TOKEN)) {
                    return input.getKey() + " = [****FILTERED****]";
                } else {
                    return input.getKey() + " = [" + Joiner.on(", ").join(input.getValue()) + "]";
                }
            }
        }

        private static class HeaderToString implements Function<Map.Entry<String, List<String>>, String> {
            @Nullable
            @Override
            public String apply(@Nullable Map.Entry<String, List<String>> input) {
                return input == null ? null : input.getKey() + " = [" + Joiner.on(", ").join(input.getValue()) + "]";
            }
        }
    }
}
