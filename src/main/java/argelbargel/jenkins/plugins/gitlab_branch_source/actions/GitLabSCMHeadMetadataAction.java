package argelbargel.jenkins.plugins.gitlab_branch_source.actions;


import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead;
import jenkins.scm.api.metadata.ObjectMetadataAction;

import java.util.Objects;


public class GitLabSCMHeadMetadataAction extends ObjectMetadataAction {
    private final int projectId;
    private final String ref;
    private final String hash;

    public GitLabSCMHeadMetadataAction(GitLabSCMHead head, String url) {
        this(head, url, head.getRevision().getHash());
    }

    public GitLabSCMHeadMetadataAction(GitLabSCMHead head, String url, String hash) {
        super(head.getName(), "", url);
        this.projectId = head.getProjectId();
        this.ref = head.getRef();
        this.hash = hash;
    }

    public int getProjectId() {
        return projectId;
    }

    public String getRef() {
        return ref;
    }

    public String getHash() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GitLabSCMHeadMetadataAction)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        GitLabSCMHeadMetadataAction that = (GitLabSCMHeadMetadataAction) o;
        return getProjectId() == that.getProjectId() &&
                Objects.equals(getRef(), that.getRef()) &&
                Objects.equals(getHash(), that.getHash());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getProjectId(), getRef(), getHash());
    }
}
