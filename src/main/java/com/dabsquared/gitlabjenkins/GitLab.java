package com.dabsquared.gitlabjenkins;

import org.gitlab.api.GitlabAPI;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitLab {
    public static final String NAME = "gitlab-plugin";

    private static final Logger LOGGER = Logger.getLogger(GitLab.class.getName());
    private GitlabAPI api;

    public static boolean checkConnection(String token, String url, boolean ignoreCertificateErrors) throws IOException {
        GitlabAPI testApi = GitlabAPI.connect(url, token);
        testApi.ignoreCertificateErrors(ignoreCertificateErrors);
        testApi.getProjects();
        return true;
    }

    public GitlabAPI instance() {
        if (api == null) {
            String token = GitLabPushTrigger.getDesc().getGitlabApiToken();
            String url = GitLabPushTrigger.getDesc().getGitlabHostUrl();
            boolean ignoreCertificateErrors = GitLabPushTrigger.getDesc().getIgnoreCertificateErrors();
            LOGGER.log(Level.FINE, "Connecting to Gitlab server ({0})", url);
            api = GitlabAPI.connect(url, token);
            api.ignoreCertificateErrors(ignoreCertificateErrors);
        }

        return api;
    }
}
