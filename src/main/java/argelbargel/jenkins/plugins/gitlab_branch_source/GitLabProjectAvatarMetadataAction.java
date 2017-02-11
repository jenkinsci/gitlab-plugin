package argelbargel.jenkins.plugins.gitlab_branch_source;


import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMIcons.Size;
import jenkins.scm.api.metadata.AvatarMetadataAction;
import org.gitlab.api.models.GitlabProject;

import javax.annotation.Nonnull;
import java.util.Objects;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMIcons.avatarFileName;

class GitLabProjectAvatarMetadataAction extends AvatarMetadataAction {
    private final GitlabProject project;
    private final String connectionName;

    GitLabProjectAvatarMetadataAction(GitlabProject project, String connectionName) {
        this.project = project;
        this.connectionName = connectionName;
    }

    @Override
    public String getAvatarImageOf(@Nonnull String size) {
        return avatarFileName(project, connectionName, Size.byDimensions(size));
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GitLabProjectAvatarMetadataAction)) {
            return false;
        }
        GitLabProjectAvatarMetadataAction that = (GitLabProjectAvatarMetadataAction) o;
        return Objects.equals(project, that.project);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project);
    }
}
