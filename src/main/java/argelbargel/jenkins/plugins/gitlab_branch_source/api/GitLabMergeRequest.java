package argelbargel.jenkins.plugins.gitlab_branch_source.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.gitlab.api.models.GitlabMergeRequest;

@SuppressWarnings("unused")
public class GitLabMergeRequest extends GitlabMergeRequest {
    private String sha;
    @JsonProperty("force_remove_source_branch")
    private boolean removeSourceBranch;
    @JsonProperty("work_in_progress")
    private boolean wip;

    public void setSha(String sha) {
        this.sha = sha;
    }

    public String getSha() {
        return sha;
    }

    public void setWorkInProgress(boolean wip) {
        this.wip = wip;
    }

    public boolean isWorkInProgress() {
        return wip;
    }

    public void setRemoveSourceBranch(boolean value) {
        removeSourceBranch = value;
    }

    public boolean getRemoveSourceBranch() {
        return removeSourceBranch;
    }
}
