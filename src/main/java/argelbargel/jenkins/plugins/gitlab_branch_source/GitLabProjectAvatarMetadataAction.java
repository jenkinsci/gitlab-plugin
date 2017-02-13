package argelbargel.jenkins.plugins.gitlab_branch_source;


import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMIcons.Size;
import jenkins.scm.api.metadata.AvatarMetadataAction;

import javax.annotation.Nonnull;
import java.util.Objects;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMIcons.avatarFileName;

class GitLabProjectAvatarMetadataAction extends AvatarMetadataAction {
    private final int projectId;
    private final String connectionName;

    GitLabProjectAvatarMetadataAction(int projectId, String connectionName) {
        this.projectId = projectId;
        this.connectionName = connectionName;
    }

    @Override
    public String getAvatarImageOf(@Nonnull String size) {
        return avatarFileName(projectId, connectionName, Size.byDimensions(size));
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
        return projectId == that.projectId &&
                Objects.equals(connectionName, that.connectionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, connectionName);
    }
}
