package argelbargel.jenkins.plugins.gitlab_branch_source;


import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabProject;
import hudson.model.Action;
import org.gitlab.api.models.GitlabProject;
import org.jenkins.ui.icon.IconSpec;
import org.kohsuke.stapler.Stapler;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Objects;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMIcons.ICON_BRANCH;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMIcons.ICON_COMMIT;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMIcons.ICON_MERGE_REQUEST;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMIcons.ICON_PROJECT;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMIcons.ICON_TAG;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMIcons.Size.MEDIUM;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMIcons.iconFileName;


// TODO: remove duplicate check for pronoun != null
class GitLabLinkAction implements Action, IconSpec {
    static GitLabLinkAction create(@Nonnull String displayName, String iconFileName, @Nonnull String url) {
        return new GitLabLinkAction(displayName, iconFileName, url);
    }

    static GitLabLinkAction toProject(GitlabProject project) {
        return create(Messages.GitLabLink_DisplayName_Project(), ICON_PROJECT, project.getWebUrl());
    }

    static GitLabLinkAction toBranch(GitlabProject project, String branchName) {
        return create(project, Messages.GitLabLink_DisplayName_Branch(), ICON_BRANCH, "tree/" + branchName);
    }


    static GitLabLinkAction toTag(GitLabProject project, String tagName) {
        return create(project, Messages.GitLabLink_DisplayName_Tag(), ICON_TAG, "tree/" + tagName);
    }


    static GitLabLinkAction toCommit(GitlabProject project, String hash) {
        return create(project,
                Messages.GitLabLink_DisplayName_Commit(),
                ICON_COMMIT,
                "commits/" + hash);
    }

    static GitLabLinkAction toMergeRequest(GitlabProject project, String id) {
        return create(project,
                Messages.GitLabLink_DisplayName_MergeRequest(),
                ICON_MERGE_REQUEST,
                "merge_requests/" + String.valueOf(id));
    }

    private static GitLabLinkAction create(@Nonnull GitlabProject project, @CheckForNull String displayName, @Nonnull String iconName, String path) {
        return new GitLabLinkAction(
                displayName == null ? "" : displayName,
                iconName,
                project.getWebUrl() + "/" + path);
    }

    private static GitLabLinkAction toTree(GitlabProject project, String path) {
        return create(project, "", ICON_BRANCH, "tree/" + path);
    }

    static String treeUrl(GitlabProject project, String path) {
        return toTree(project, path).getUrlName();
    }

    static String mergeRequestUrl(GitlabProject project, String id) {
        return create(project,
                "",
                "merge_requests", id).getUrlName();
    }


    private final String displayName;
    private final String iconName;
    private final String url;

    private GitLabLinkAction(@Nonnull String displayName, String iconName, @Nonnull String url) {
        this.displayName = displayName.startsWith(Messages.GitLabLink_DisplayName_Prefix()) ? displayName : Messages.GitLabLink_DisplayName_Prefix() + " " + displayName;
        this.iconName = iconName;
        this.url = url;
    }

    @Override
    public String getIconFileName() {
        // TODO: why do we have to remove the context-path (e.g. /jenkins) here?
        return iconFileName(iconName, MEDIUM).replaceFirst("^" + Stapler.getCurrentRequest().getContextPath(), "");
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
