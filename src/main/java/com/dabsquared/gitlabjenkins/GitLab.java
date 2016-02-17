package com.dabsquared.gitlabjenkins;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.dabsquared.gitlabjenkins.connection.GitLabConnection;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.gitlab.api.GitlabAPI;

public class GitLab {
  private static final Logger LOGGER = Logger.getLogger(GitLab.class.getName());
  private GitlabAPI api;

  public GitlabAPI instance() {
    if (api == null) {
        // Use the first configured connection as work around for the GitLabProjectBranchesService
        GitLabConnectionConfig connectionConfig = (GitLabConnectionConfig) Jenkins.getInstance().getDescriptor(GitLabConnectionConfig.class);
        if (connectionConfig != null && connectionConfig.getConnections().size() > 0) {
            GitLabConnection connection = connectionConfig.getConnections().get(0);
            api = GitlabAPI.connect(connection.getUrl(), connection.getApiToken());
            api.ignoreCertificateErrors(connection.isIgnoreCertificateErrors());
        }
    }

    return api;
  }
  
  public static boolean checkConnection (String token, String url, boolean ignoreCertificateErrors) throws IOException {
	  GitlabAPI testApi = GitlabAPI.connect(url, token);
	  testApi.ignoreCertificateErrors(ignoreCertificateErrors);
	  testApi.getProjects();
	  return true;
  }
}
