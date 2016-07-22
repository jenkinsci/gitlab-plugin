package com.dabsquared.gitlabjenkins.gitlab;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.dabsquared.gitlabjenkins.connection.GitLabApiToken;
import com.dabsquared.gitlabjenkins.connection.GitLabConnection;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.util.JsonUtil;
import com.dabsquared.gitlabjenkins.util.LoggerUtil;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import hudson.ProxyConfiguration;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Item;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
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
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

/**
 * @author Robin Müller
 */
public class GitLabClientBuilder {

    private final static Logger LOGGER = Logger.getLogger(GitLabClientBuilder.class.getName());
    private static final String PRIVATE_TOKEN = "PRIVATE-TOKEN";

    public static GitLabApi buildClient(String gitlabHostUrl, final String gitlabApiTokenId, boolean ignoreCertificateErrors, int connectionTimeout, int readTimeout) {
        ResteasyClientBuilder builder = new ResteasyClientBuilder();
        if (ignoreCertificateErrors) {
            builder.hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY);
            builder.disableTrustManager();
        }
        ProxyConfiguration proxyConfiguration = Jenkins.getActiveInstance().proxy;
        Proxy proxy = proxyConfiguration ==  null ? Proxy.NO_PROXY : proxyConfiguration.createProxy(getHost(gitlabHostUrl));
        if (!proxy.equals(Proxy.NO_PROXY)) {
            InetSocketAddress address = (InetSocketAddress) proxy.address();
            builder.defaultProxy(address.getHostName().replaceFirst("^.*://", ""),
                                 address.getPort(),
                                 address.getHostName().startsWith("https") ? "https" : "http",
                                 proxyConfiguration.getUserName(),
                                 proxyConfiguration.getPassword());
        }

        return builder
            .connectionPoolSize(60)
            .maxPooledPerRoute(30)
            .establishConnectionTimeout(connectionTimeout, TimeUnit.SECONDS)
            .socketTimeout(readTimeout, TimeUnit.SECONDS)
            .register(new JacksonJsonProvider())
            .register(new JacksonConfig())
            .register(new ApiHeaderTokenFilter(getApiToken(gitlabApiTokenId)))
            .register(new LoggingFilter())
            .build().target(gitlabHostUrl)
            .proxyBuilder(GitLabApi.class)
            .classloader(Jenkins.getInstance().getPluginManager().uberClassLoader)
                .build();
    }

    public static GitLabApi buildClient(GitLabConnection connection) {
        return buildClient(connection.getUrl(),
                           connection.getApiTokenId(),
                           connection.isIgnoreCertificateErrors(),
                           connection.getConnectionTimeout(),
                           connection.getReadTimeout());
    }

    @Initializer(before = InitMilestone.PLUGINS_STARTED)
    public static void setRuntimeDelegate() {
        RuntimeDelegate.setInstance(new ResteasyProviderFactory());
    }

    private static String getHost(String gitlabUrl) {
        try {
            return new URL(gitlabUrl).getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private static String getApiToken(String apiTokenId) {
        StandardCredentials credentials = CredentialsMatchers.firstOrNull(
            lookupCredentials(StandardCredentials.class, (Item) null, ACL.SYSTEM, new ArrayList<DomainRequirement>()),
            CredentialsMatchers.withId(apiTokenId));
        if (credentials != null) {
            if (credentials instanceof GitLabApiToken) {
                return ((GitLabApiToken) credentials).getApiToken().getPlainText();
            }
            if (credentials instanceof StringCredentials) {
                return ((StringCredentials) credentials).getSecret().getPlainText();
            }
        }
        throw new IllegalStateException("No credentials found for credentialsId: " + apiTokenId);
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

    private static class ResteasyClientBuilder extends org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder {

        private CredentialsProvider proxyCredentials;

        ResteasyClientBuilder defaultProxy(String hostname, int port, final String scheme, String username, String password) {
            super.defaultProxy(hostname, port, scheme);
            if (username != null && password != null) {
                proxyCredentials = new BasicCredentialsProvider();
                proxyCredentials.setCredentials(new AuthScope(hostname, port), new UsernamePasswordCredentials(username, password));
            }
            return this;
        }

        @Override
        protected ClientHttpEngine initDefaultEngine() {
            ApacheHttpClient4Engine httpEngine = (ApacheHttpClient4Engine) super.initDefaultEngine();
            if (proxyCredentials != null) {
                ((DefaultHttpClient) httpEngine.getHttpClient()).setCredentialsProvider(proxyCredentials);
            }
            return httpEngine;
        }
    }
}
