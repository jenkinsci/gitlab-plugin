package com.dabsquared.gitlabjenkins.gitlab.api.impl;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder;
import static com.dabsquared.gitlabjenkins.webhook.ActionResolver.getSecretToken;
import hudson.Extension;
import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.ProxyClientConfig;
import org.gitlab4j.api.GitLabApi.ApiVersion;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Extension
@Restricted(NoExternalUse.class)
public final class V3GitLabClientBuilder extends GitLabClientBuilder {

    ProxyConfiguration httpProxyConfig = Jenkins.getActiveInstance().proxy;
    private static final int ORDINAL = 2;
    GitLabApi client;

    public V3GitLabClientBuilder() {
        super("V3", ORDINAL);
    }

    @Override
    public GitLabApi buildClient(
            String url, String token, boolean ignoreCertificateErrors, int connectionTimeout, int readTimeout) {
        if (httpProxyConfig != null) {
            Proxy proxy = httpProxyConfig.createProxy(getHost(url));
            InetSocketAddress address = (InetSocketAddress) proxy.address();
            String proxyHost = address.getHostString();
            int proxyPort = address.getPort();
            String proxyUri = "http://" + proxyHost + ":" + proxyPort;
                if (httpProxyConfig.getUserName() != null && httpProxyConfig.getPassword() != null) {
                    Map<String, Object> clientConfig = ProxyClientConfig.createProxyClientConfig(proxyUri, httpProxyConfig.getUserName(), httpProxyConfig.getPassword());
                    client = new GitLabApi(ApiVersion.V3, url, token, getSecretToken(), clientConfig);
                }
        }
        else {
            client = new GitLabApi(ApiVersion.V3, url, token, getSecretToken());
        }
        client.setIgnoreCertificateErrors(ignoreCertificateErrors);
        client.setRequestTimeout(connectionTimeout, readTimeout);
        return client;
    }

    private String getHost(String url) {
        try {
            return new URL(url).getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
