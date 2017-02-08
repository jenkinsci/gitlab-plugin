package argelbargel.jenkins.plugins.gitlab_branch_source.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.gitlab.api.models.GitlabGroup;

@SuppressWarnings("unused")
public class GitLabGroup extends GitlabGroup {
    @JsonProperty("avatar_url")
    private String avatarUrl;

    public void setAvatarUrl(String url) {
        this.avatarUrl = url;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
