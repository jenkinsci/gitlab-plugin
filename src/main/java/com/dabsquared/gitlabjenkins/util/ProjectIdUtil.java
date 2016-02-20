package com.dabsquared.gitlabjenkins.util;

import org.eclipse.jgit.transport.URIish;

import java.net.URISyntaxException;

/**
 * @author Robin Müller
 */
public final class ProjectIdUtil {

    private ProjectIdUtil() { }

    public static String retrieveProjectId(String remoteUrl) throws ProjectIdResolutionException {
        try {
            String projectId = new URIish(remoteUrl).getPath();
            if (projectId.startsWith("/")) {
                projectId = projectId.substring(1);
            }
            if (projectId.endsWith(".git")) {
                projectId = projectId.substring(0, projectId.lastIndexOf(".git"));
            }
            return projectId;
        } catch (URISyntaxException e) {
            throw new ProjectIdResolutionException(String.format("Failed to retrieve GitLab projectId for %s", remoteUrl), e);
        }
    }

    public static class ProjectIdResolutionException extends Exception {
        public ProjectIdResolutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
