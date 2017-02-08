package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.scm.api.metadata.ObjectMetadataAction;
import org.gitlab.api.models.GitlabProject;

import javax.annotation.Nonnull;

class GitLabProjectMetadataAction extends ObjectMetadataAction {
    private final String defaultBranch;

    GitLabProjectMetadataAction(@Nonnull GitlabProject project) {
        super(null, project.getDescription(), project.getWebUrl());
        defaultBranch = project.getDefaultBranch();
    }

    String getDefaultBranch() {
        return defaultBranch;
    }
}
