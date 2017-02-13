package argelbargel.jenkins.plugins.gitlab_branch_source;

import hudson.model.Action;
import org.gitlab.api.models.GitlabProject;
import org.jenkins.ui.icon.IconSpec;
import org.kohsuke.stapler.Stapler;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Objects;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabConnection;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMIcons.ICON_GITLAB_LOGO;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMIcons.Size.MEDIUM;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMIcons.iconFileName;


// TODO: remove duplicate check for pronoun != null
class GitLabLinkAction implements Action, IconSpec {
    static GitLabLinkAction toServer(String pronoun, String connectionName) {
        return new GitLabLinkAction(
                pronoun != null ? pronoun : "",
                gitLabConnection(connectionName).getUrl());
    }

    static GitLabLinkAction toProject(String pronoun, GitlabProject project) {
        return new GitLabLinkAction(
                pronoun != null ? pronoun : "",
                project.getWebUrl());
    }

    static GitLabLinkAction toCommit(GitlabProject project, String path) {
        return toProject(project,
                Messages.GitLabLink_DisplayName_Commit(),
                "commits", path);
    }

    static GitLabLinkAction toTree(@CheckForNull String pronoun, GitlabProject project, String path) {
        return toProject(project, pronoun, "tree", path);
    }

    static GitLabLinkAction toMergeRequest(@CheckForNull String pronoun, GitlabProject project, int id) {
        return toProject(project,
                pronoun,
                "merge_requests", String.valueOf(id));
    }

    private static GitLabLinkAction toProject(@Nonnull GitlabProject project, @CheckForNull String displayName, String what, String path) {
        return new GitLabLinkAction(
                displayName == null ? "" : displayName,
                project.getWebUrl() + "/" + what + "/" + path);
    }

    static String treeUrl(GitlabProject project, String path) {
        return toTree("", project, path).getUrlName();
    }

    static String mergeRequestUrl(GitlabProject project, String id) {
        return toProject(project,
                "",
                "merge_requests", id).getUrlName();
    }


    private final String displayName;
    private final String url;

    private GitLabLinkAction(@Nonnull String displayName, @Nonnull String url) {
        this.displayName = displayName.startsWith(Messages.GitLabLink_DisplayName_Prefix()) ? displayName : Messages.GitLabLink_DisplayName_Prefix() + " " + displayName;
        this.url = url;
    }

    @Override
    public String getIconFileName() {
        String iconFileName = iconFileName(ICON_GITLAB_LOGO, MEDIUM);
        // TODO: why do we have to remove the context-path (e.g. /jenkins) here?
        return (iconFileName != null) ? iconFileName.replaceFirst("^" + Stapler.getCurrentRequest().getContextPath(), "") : null;
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
        GitLabLinkAction that = (GitLabLinkAction) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}
