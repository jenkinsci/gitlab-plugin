package argelbargel.jenkins.plugins.gitlab_branch_source;

import hudson.model.Action;
import org.gitlab.api.models.GitlabProject;
import org.jenkins.ui.icon.IconSpec;

import java.util.Objects;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabConnection;
import static argelbargel.jenkins.plugins.gitlab_branch_source.Icons.ICON_GITLAB_LOGO;
import static argelbargel.jenkins.plugins.gitlab_branch_source.Icons.Size.MEDIUM;
import static argelbargel.jenkins.plugins.gitlab_branch_source.Icons.iconFileName;

class GitLabLink implements Action, IconSpec {
    static GitLabLink toServer(String connectionName) {
        return new GitLabLink(
                Messages.GitLabLink_DisplayName_Server(),
                gitLabConnection(connectionName).getUrl());
    }

    static GitLabLink toProject(GitlabProject project) {
        return new GitLabLink(
                Messages.GitLabLink_DisplayName_Project(),
                project.getWebUrl());
    }

    static GitLabLink toCommit(GitlabProject project, String path) {
        return toProject(project,
                Messages.GitLabLink_DisplayName_Commit(),
                "commits", path);
    }

    static GitLabLink toTree(GitlabProject project, String path) {
        return toProject(project,
                Messages.GitLabLink_DisplayName_Tree(),
                "tree", path);
    }

    static GitLabLink toMergeRequest(GitlabProject project, String id) {
        return toProject(project,
                Messages.GitLabLink_DisplayName_MergeRequest(),
                "merge_requests", String.valueOf(id));
    }

    private static GitLabLink toProject(GitlabProject project, String displayName, String what, String path) {
        return new GitLabLink(
                displayName,
                project.getWebUrl() + "/" + what + "/" + path);
    }

    static String treeUrl(GitlabProject project, String path) {
        return toTree(project, path).getUrlName();
    }

    static String mergeRequestUrl(GitlabProject project, String id) {
        return toMergeRequest(project, id).getUrlName();
    }


    private final String displayName;
    private final String url;

    private GitLabLink(String displayName, String url) {
        this.displayName = displayName;
        this.url = url;
    }

    @Override
    public String getIconFileName() {
        return iconFileName(ICON_GITLAB_LOGO, MEDIUM);
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getUrlName() {
        return url;
    }

    @Override
    public String getIconClassName() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GitLabLink that = (GitLabLink) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}
