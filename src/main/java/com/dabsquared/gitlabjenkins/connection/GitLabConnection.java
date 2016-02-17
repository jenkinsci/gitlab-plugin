package com.dabsquared.gitlabjenkins.connection;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Robin Müller
 */
public class GitLabConnection {

    private final String name;
    private final String url;
    private final String apiToken;
    private final boolean ignoreCertificateErrors;

    @DataBoundConstructor
    public GitLabConnection(String name, String url, String apiToken, boolean ignoreCertificateErrors) {
        this.name = name;
        this.url = url;
        this.apiToken = apiToken;
        this.ignoreCertificateErrors = ignoreCertificateErrors;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getApiToken() {
        return apiToken;
    }

    public boolean isIgnoreCertificateErrors() {
        return ignoreCertificateErrors;
    }
}
