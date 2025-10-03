package com.dabsquared.gitlabjenkins.gitlab.api.impl;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static java.net.Proxy.Type.HTTP;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.dabsquared.gitlabjenkins.connection.GitLabApiToken;
import com.dabsquared.gitlabjenkins.connection.GitlabCredentialResolver;
import com.dabsquared.gitlabjenkins.gitlab.JacksonConfig;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import com.dabsquared.gitlabjenkins.util.JsonUtil;
import com.dabsquared.gitlabjenkins.util.LoggerUtil;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ProxyConfiguration;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.RuntimeDelegate;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngineBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;
import org.jboss.resteasy.plugins.providers.JaxrsFormProvider;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Restricted(NoExternalUse.class)
public class ResteasyGitLabClientBuilder extends GitLabClientBuilder {
    private static final Logger LOGGER = Logger.getLogger(ResteasyGitLabClientBuilder.class.getName());
    private static final String PRIVATE_TOKEN = "PRIVATE-TOKEN";

    @Initializer(before = InitMilestone.PLUGINS_STARTED)
    public static void setRuntimeDelegate() {
        RuntimeDelegate.setInstance(new ResteasyProviderFactoryImpl());
    }

    private final Class<? extends GitLabApiProxy> apiProxyClass;
    private final Function<MergeRequest, Integer> mergeRequestIdProvider;

    ResteasyGitLabClientBuilder(
            String id,
            int ordinal,
            Class<? extends GitLabApiProxy> apiProxyClass,
            Function<MergeRequest, Integer> mergeRequestIdProvider) {
        super(id, ordinal);
        this.apiProxyClass = apiProxyClass;
        this.mergeRequestIdProvider = mergeRequestIdProvider;
    }

    @NonNull
    @Override
    public final GitLabClient buildClient(
            String url,
            GitlabCredentialResolver credentialResolver,
            boolean ignoreCertificateErrors,
            int connectionTimeout,
            int readTimeout) {
        return buildClient(
                url,
                credentialResolver,
                Jenkins.getActiveInstance().proxy,
                ignoreCertificateErrors,
                connectionTimeout,
                readTimeout);
    }

    private GitLabClient buildClient(
            String url,
            GitlabCredentialResolver credentialResolver,
            ProxyConfiguration httpProxyConfig,
            boolean ignoreCertificateErrors,
            int connectionTimeout,
            int readTimeout) {
        ResteasyClientBuilder builder = new ResteasyClientBuilderImpl();

        if (ignoreCertificateErrors) {
            builder.hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY);
            builder.disableTrustManager();
        }

        if (httpProxyConfig != null) {
            Proxy proxy = httpProxyConfig.createProxy(getHost(url));
            if (proxy.type() == HTTP) {
                InetSocketAddress address = (InetSocketAddress) proxy.address();
                String hostname = address.getHostString().replaceFirst("^.*://", "");
                builder.defaultProxy(
                        hostname, address.getPort(), address.getHostName().startsWith("https") ? "https" : "http");

                if (httpProxyConfig.getUserName() != null && httpProxyConfig.getPassword() != null) {
                    CredentialsProvider proxyCredentials = new BasicCredentialsProvider();
                    proxyCredentials.setCredentials(
                            new AuthScope(hostname, address.getPort()),
                            new UsernamePasswordCredentials(
                                    httpProxyConfig.getUserName(), httpProxyConfig.getPassword()));

                    ClientHttpEngine httpEngine = new ClientHttpEngineBuilder43(proxyCredentials)
                            .resteasyClientBuilder(builder)
                            .build();
                    builder.httpEngine(httpEngine);
                }
            }
        }

        ResteasyWebTarget target = (ResteasyWebTarget) builder.connectionPoolSize(60)
                .maxPooledPerRoute(30)
                .connectTimeout(connectionTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .register(new JacksonFeature())
                .register(new JacksonConfig())
                .register(new ApiHeaderTokenFilter(credentialResolver, url))
                .register(new LoggingFilter())
                .register(new RemoveAcceptEncodingFilter())
                .register(new JaxrsFormProvider())
                .build()
                .target(url);
        // ProxyBuilderImpl must be accessible by context class loader
        ClassLoader ctx = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        GitLabApiProxy apiProxy = target.proxyBuilder(apiProxyClass)
                .classloader(apiProxyClass.getClassLoader())
                .build();
        Thread.currentThread().setContextClassLoader(ctx);

        return new ResteasyGitLabClient(url, apiProxy, mergeRequestIdProvider);
    }

    private String getHost(String url) {
        try {
            return new URL(url).getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Priority(Priorities.HEADER_DECORATOR)
    private static class ApiHeaderTokenFilter implements ClientRequestFilter {
        private final GitlabCredentialResolver credentialResolver;
        private final String url;

        ApiHeaderTokenFilter(GitlabCredentialResolver credentialResolver, String url) {
            this.credentialResolver = credentialResolver;
            this.url = url;
        }

        @Restricted(NoExternalUse.class)
        private String getApiToken(GitlabCredentialResolver credentialResolver) {
            Item item = credentialResolver.getItem();
            ItemGroup<?> context = item != null ? item.getParent() : Jenkins.get();
            StandardCredentials credentials = CredentialsMatchers.firstOrNull(
                    lookupCredentials(
                            StandardCredentials.class,
                            context,
                            ACL.SYSTEM,
                            URIRequirementBuilder.fromUri(url).build()),
                    CredentialsMatchers.withId(credentialResolver.getCredentialsId()));

            if (item != null) {
                com.cloudbees.plugins.credentials.CredentialsProvider.track(item, credentials);
            }

            if (credentials != null) {
                if (credentials instanceof GitLabApiToken token) {
                    return token.getApiToken().getPlainText();
                }
                if (credentials instanceof StringCredentials stringCredentials) {
                    return stringCredentials.getSecret().getPlainText();
                }
            }
            throw new IllegalStateException(
                    "No credentials found for credentialsId: " + credentialResolver.getCredentialsId());
        }

        public void filter(ClientRequestContext requestContext) {
            requestContext.getHeaders().putSingle(PRIVATE_TOKEN, getApiToken(credentialResolver));
        }
    }

    @Priority(Priorities.USER)
    private static class LoggingFilter implements ClientRequestFilter, ClientResponseFilter {
        @Override
        public void filter(ClientRequestContext context) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(
                        Level.FINEST,
                        "Call GitLab:\nHTTP method: {0}\nURL: {1}\nRequest headers: [\n{2}\n]",
                        LoggerUtil.toArray(
                                context.getMethod(), context.getUri(), toFilteredString(context.getStringHeaders())));
            }
        }

        @Override
        public void filter(ClientRequestContext request, ClientResponseContext response) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(
                        Level.FINEST,
                        "Got response from GitLab:\nURL: {0}\nStatus: {1} {2}\nResponse headers: [\n{3}\n]\nResponse body: {4}",
                        LoggerUtil.toArray(
                                request.getUri(),
                                response.getStatus(),
                                response.getStatusInfo(),
                                toString(response.getHeaders()),
                                getPrettyPrintResponseBody(response)));
            }
        }

        private String toFilteredString(MultivaluedMap<String, String> headers) {
            return headers.entrySet().stream().map(new HeaderToFilteredString()).collect(Collectors.joining(",\n"));
        }

        private String toString(MultivaluedMap<String, String> headers) {
            return headers.entrySet().stream().map(new HeaderToString()).collect(Collectors.joining(",\n"));
        }

        private String getPrettyPrintResponseBody(ClientResponseContext responseContext) {
            String responseBody = getResponseBody(responseContext);
            if (StringUtils.isNotEmpty(responseBody)
                    && responseContext.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)) {
                return JsonUtil.toPrettyPrint(responseBody);
            }
            return responseBody;
        }

        private String getResponseBody(ClientResponseContext context) {
            // Cannot use try-with-resources here because the stream needs to remain open for reading later. Instead, we
            // reset it when done.
            try {
                InputStream entityStream = context.getEntityStream();
                if (entityStream != null && entityStream.markSupported()) {
                    byte[] bytes = IOUtils.toByteArray(entityStream);
                    entityStream.reset();
                    return new String(bytes, StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failure during reading the response body", e);
            }
            return "";
        }

        private static class HeaderToFilteredString implements Function<Map.Entry<String, List<String>>, String> {
            @CheckForNull
            @Override
            public String apply(@CheckForNull Map.Entry<String, List<String>> input) {
                if (input == null) {
                    return null;
                } else if (input.getKey().equals(PRIVATE_TOKEN)) {
                    return input.getKey() + " = [****FILTERED****]";
                } else {
                    return input.getKey() + " = [" + input.getValue().stream().collect(Collectors.joining(", ")) + "]";
                }
            }
        }

        private static class HeaderToString implements Function<Map.Entry<String, List<String>>, String> {
            @CheckForNull
            @Override
            public String apply(Map.Entry<String, List<String>> input) {
                return input == null
                        ? null
                        : input.getKey() + " = [" + input.getValue().stream().collect(Collectors.joining(", ")) + "]";
            }
        }
    }

    @Priority(Priorities.HEADER_DECORATOR)
    private static class RemoveAcceptEncodingFilter implements ClientRequestFilter {
        RemoveAcceptEncodingFilter() {}

        @Override
        public void filter(ClientRequestContext clientRequestContext) {
            clientRequestContext.getHeaders().remove("Accept-Encoding");
        }
    }

    private static class ClientHttpEngineBuilder43
            extends org.jboss.resteasy.client.jaxrs.engines.ClientHttpEngineBuilder43 {

        private final CredentialsProvider proxyCredentials;
        private ResteasyClientBuilder that;

        private ClientHttpEngineBuilder43(CredentialsProvider proxyCredentials) {
            this.proxyCredentials = proxyCredentials;
        }

        @Override
        public ClientHttpEngineBuilder resteasyClientBuilder(ResteasyClientBuilder resteasyClientBuilder) {
            super.resteasyClientBuilder(resteasyClientBuilder);
            that = resteasyClientBuilder;
            return this;
        }

        @Override
        protected ClientHttpEngine createEngine(
                final HttpClientConnectionManager cm,
                final RequestConfig.Builder rcBuilder,
                final HttpHost defaultProxy,
                final int responseBufferSize,
                final HostnameVerifier verifier,
                final SSLContext theContext) {
            final HttpClient httpClient;
            rcBuilder.setProxy(new HttpHost(
                    that.getDefaultProxyHostname(), that.getDefaultProxyPort(), that.getDefaultProxyScheme()));
            if (System.getSecurityManager() == null) {
                httpClient = HttpClientBuilder.create()
                        .setConnectionManager(cm)
                        .setDefaultCredentialsProvider(proxyCredentials)
                        .setDefaultRequestConfig(rcBuilder.build())
                        .disableContentCompression()
                        .build();
            } else {
                httpClient = AccessController.doPrivileged(new PrivilegedAction<HttpClient>() {
                    @Override
                    public HttpClient run() {
                        return HttpClientBuilder.create()
                                .setConnectionManager(cm)
                                .setDefaultCredentialsProvider(proxyCredentials)
                                .setDefaultRequestConfig(rcBuilder.build())
                                .disableContentCompression()
                                .build();
                    }
                });
            }

            ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient, true);
            engine.setResponseBufferSize(responseBufferSize);
            engine.setHostnameVerifier(verifier);
            engine.setSslContext(theContext);
            engine.setFollowRedirects(that.isFollowRedirects());
            return engine;
        }
    }
}
