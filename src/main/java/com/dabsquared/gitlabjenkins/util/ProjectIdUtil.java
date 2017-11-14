package com.dabsquared.gitlabjenkins.util;


import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import org.eclipse.jgit.transport.URIish;

import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Robin Müller
 */
public final class ProjectIdUtil {

    private static final Pattern PROJECT_ID_PATTERN = Pattern.compile("^/?(?<projectId>.*)(\\.git)$");

    private ProjectIdUtil() { }

    public static String retrieveProjectId(GitLabClient client, String remoteUrl) throws ProjectIdResolutionException {
        try {
            String baseUri = client.getHostUrl();
            String projectId;
            if (baseUri != null && remoteUrl.startsWith(baseUri)) {
                projectId = new URIish(remoteUrl.substring(baseUri.length(), remoteUrl.length())).getPath();
            } else {
                projectId = new URIish(remoteUrl).getPath();
            }
            if (projectId.startsWith(":")) {
                projectId = projectId.substring(1);
            }
            
            Matcher matcher = PROJECT_ID_PATTERN.matcher(projectId);
            if (matcher.matches()) {
                return matcher.group("projectId");
            } else {
                throw new ProjectIdResolutionException(String.format("Failed to retrieve GitLab projectId for %s", remoteUrl));
            }
        } catch (URISyntaxException e) {
            throw new ProjectIdResolutionException(String.format("Failed to retrieve GitLab projectId for %s", remoteUrl), e);
        }
    }

    public static class ProjectIdResolutionException extends Exception {
        public ProjectIdResolutionException(String message, Throwable cause) {
            super(message, cause);
        }

        public ProjectIdResolutionException(String message) {
            super(message);
        }
    }
}
