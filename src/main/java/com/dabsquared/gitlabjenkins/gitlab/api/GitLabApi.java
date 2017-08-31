package com.dabsquared.gitlabjenkins.gitlab.api;

/**
 * Extends REST-client interface to provide additional methods for plugin's code.
 * 
 * @author Alexander Leshkin
 *
 */
public interface GitLabApi extends GitLabApiClient {
    /**
     * Returns GitLab host base url from plugin confugruation.
     */
    String getGitLabHostUrl();
}
