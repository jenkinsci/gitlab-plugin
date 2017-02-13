package argelbargel.jenkins.plugins.gitlab_branch_source;


import jenkins.scm.api.metadata.ObjectMetadataAction;
import org.gitlab.api.models.GitlabProject;

import javax.annotation.Nonnull;


class GitLabProjectMetadataAction extends ObjectMetadataAction {
    GitLabProjectMetadataAction(@Nonnull GitlabProject project) {
        super(null, project.getDescription(), project.getWebUrl());
    }
}
