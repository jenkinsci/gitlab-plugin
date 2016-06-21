package com.dabsquared.gitlabjenkins.util;

import org.eclipse.jgit.transport.URIish;

import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Robin Müller
 */
public final class ProjectIdUtil {

    private static final Pattern PROJECT_ID_PATTERN = Pattern.compile("^/?(.*/)?(?<projectId>.*/.*)(\\.git)?$");

    private ProjectIdUtil() { }

    public static String retrieveProjectId(String remoteUrl) throws ProjectIdResolutionException {
        try {
            String projectId = new URIish(remoteUrl).getPath();
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
