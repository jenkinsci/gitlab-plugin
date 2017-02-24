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


class GitLabLinkAction implements Action, IconSpec {
    @Nonnull
    static GitLabLinkAction create(@CheckForNull String displayName, @Nonnull String iconFileName, @Nonnull String url) {
        if (displayName == null) {
            return create("", iconFileName, url);
        }
        
        return new GitLabLinkAction(displayName, iconFileName, url);
    }

    @Nonnull
    static GitLabLinkAction toProject(GitlabProject project) {
        return create(Messages.GitLabLink_DisplayName_Project(), ICON_PROJECT, project.getWebUrl());
    }

    @Nonnull
    static GitLabLinkAction toBranch(GitlabProject project, String branchName) {
        return create(Messages.GitLabLink_DisplayName_Branch(), ICON_BRANCH, project, "tree/" + branchName);
    }

    @Nonnull
    static GitLabLinkAction toTag(GitLabProject project, String tagName) {
        return create(Messages.GitLabLink_DisplayName_Tag(), ICON_TAG, project, "tree/" + tagName);
    }

    @Nonnull
    static GitLabLinkAction toCommit(GitlabProject project, String hash) {
        return create(Messages.GitLabLink_DisplayName_Commit(), ICON_COMMIT, project,
                "commits/" + hash);
    }

    @Nonnull
    static GitLabLinkAction toMergeRequest(GitlabProject project, String id) {
        return create(Messages.GitLabLink_DisplayName_MergeRequest(), ICON_MERGE_REQUEST, project,
                "merge_requests/" + String.valueOf(id));
    }

    @Nonnull
    private static GitLabLinkAction create(@CheckForNull String displayName, @Nonnull String iconName, @Nonnull GitlabProject project, String path) {
        return new GitLabLinkAction(
                displayName == null ? "" : displayName,
                iconName,
                project.getWebUrl() + "/" + path);
    }


    private final String displayName;
    private final String iconName;
    private final String url;

    private GitLabLinkAction(@Nonnull String displayName, @Nonnull String iconName, @Nonnull String url) {
        this.displayName = displayName.startsWith(Messages.GitLabLink_DisplayName_Prefix()) ? displayName : Messages.GitLabLink_DisplayName_Prefix() + " " + displayName;
        this.iconName = iconName;
        this.url = url;
    }

    @Override
    public String getIconFileName() {
        String iconFileName = iconFileName(iconName, MEDIUM);
        // TODO: why do we have to remove the context-path (e.g. /jenkins) here?
        return iconFileName != null ? iconFileName.replaceFirst("^" + Stapler.getCurrentRequest().getContextPath(), "") : null;
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
