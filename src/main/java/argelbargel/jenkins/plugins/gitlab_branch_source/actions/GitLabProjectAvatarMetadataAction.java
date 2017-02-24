package argelbargel.jenkins.plugins.gitlab_branch_source.actions;


import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMIcons.Size;
import jenkins.scm.api.metadata.AvatarMetadataAction;

import javax.annotation.Nonnull;
import java.util.Objects;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMIcons.avatarFileName;


public final class GitLabProjectAvatarMetadataAction extends AvatarMetadataAction {
    private static final long serialVersionUID = 1L;

    private final int projectId;
    private final String connectionName;
    private transient String avatarUrlCache;

    public GitLabProjectAvatarMetadataAction(int projectId, String connectionName) {
        this.projectId = projectId;
        this.connectionName = connectionName;
        this.avatarUrlCache = null;
    }

    @Override
    public String getAvatarImageOf(@Nonnull String size) {
        if (avatarUrlCache == null) {
            avatarUrlCache = avatarFileName(projectId, connectionName, Size.byDimensions(size));
        }

        return avatarUrlCache;
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
