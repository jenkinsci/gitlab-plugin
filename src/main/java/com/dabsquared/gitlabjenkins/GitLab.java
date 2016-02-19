package com.dabsquared.gitlabjenkins;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gitlab.api.GitlabAPI;

public class GitLab {
  private static final Logger LOGGER = Logger.getLogger(GitLab.class.getName());
  private GitlabAPI api;

	private String gitlabApiToken;
	private String gitlabHostUrl = "";
	private boolean ignoreCertificateErrors;

	public GitLab(String gitlabApiToken, String gitlabHostUrl, boolean ignoreCertificateErrors) {
		this.gitlabApiToken = gitlabApiToken;
		this.gitlabHostUrl = gitlabHostUrl;
		this.ignoreCertificateErrors = ignoreCertificateErrors;
	}

	public GitlabAPI instance() {
    if (api == null) {
        LOGGER.log(Level.FINE, "Connecting to Gitlab server ({0})", gitlabHostUrl);
        api = GitlabAPI.connect(gitlabHostUrl, gitlabApiToken);
        api.ignoreCertificateErrors(ignoreCertificateErrors);
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