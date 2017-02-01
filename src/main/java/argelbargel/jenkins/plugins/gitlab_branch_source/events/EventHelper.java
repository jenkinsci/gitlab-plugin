package argelbargel.jenkins.plugins.gitlab_branch_source.events;

import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMNavigator;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabProjectVisibility;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.SystemHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import org.apache.commons.lang.StringUtils;
import org.gitlab.api.models.GitlabProject;

import java.io.IOException;
import java.util.logging.Logger;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabAPI;
import static argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabProjectVisibility.ALL;

class EventHelper {
    private static final Logger LOGGER = Logger.getLogger(EventHelper.class.getName());

    static GitlabProject getMatchingProject(GitLabSCMNavigator navigator, SystemHook hook) {
        return getMatchingProject(navigator,hook.getProjectVisibility(), hook.getPathWithNamespace(), hook.getProjectId());
    }

    static GitlabProject getMatchingProject(GitLabSCMNavigator navigator, PushHook hook) {
        Project project = hook.getProject();
        return getMatchingProject(navigator, GitLabProjectVisibility.byLevel(project.getVisibilityLevel()).id(), project.getPathWithNamespace(), hook.getProjectId());
    }


    private static GitlabProject getMatchingProject(GitLabSCMNavigator navigator, String visibility, String path, Integer id) {
        if (!ALL.id().equals(navigator.getProjectVisibilityId()) && visibility.equalsIgnoreCase(navigator.getProjectVisibilityId())) {
            return null;
        }

        if (!StringUtils.isEmpty(navigator.getProjectSearchPattern()) && path.contains(navigator.getProjectSearchPattern().toLowerCase())) {
            return null;
        }

        return getGitlabProject(navigator, id);
    }

    private static GitlabProject getGitlabProject(GitLabSCMNavigator navigator, Integer id) {
        try {
            return gitLabAPI(navigator.getConnectionName()).getProject(id);
        } catch (IOException e) {
            LOGGER.info("could not get project with id " + id);
            return null;
        }
    }
}
