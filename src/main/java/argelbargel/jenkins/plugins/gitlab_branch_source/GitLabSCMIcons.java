package argelbargel.jenkins.plugins.gitlab_branch_source;


import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPIException;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabGroup;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabProject;
import hudson.init.Initializer;
import jenkins.model.Jenkins;
import org.apache.commons.jelly.JellyContext;
import org.gitlab.api.models.GitlabNamespace;
import org.gitlab.api.models.GitlabProject;
import org.jenkins.ui.icon.Icon;
import org.kohsuke.stapler.Stapler;

import java.util.NoSuchElementException;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabAPI;
import static org.jenkins.ui.icon.Icon.ICON_LARGE_STYLE;
import static org.jenkins.ui.icon.Icon.ICON_MEDIUM_STYLE;
import static org.jenkins.ui.icon.Icon.ICON_SMALL_STYLE;
import static org.jenkins.ui.icon.Icon.ICON_XLARGE_STYLE;
import static org.jenkins.ui.icon.IconSet.icons;


public final class GitLabSCMIcons {
    enum Size {
        SMALL("icon-sm", "16x16", ICON_SMALL_STYLE),
        MEDIUM("icon-md", "24x24", ICON_MEDIUM_STYLE),
        LARGE("icon-lg", "32x32", ICON_LARGE_STYLE),
        XLARGE("icon-xlg", "48x48", ICON_XLARGE_STYLE);

        static Size byDimensions(String dimensions) {
            for (Size s : values()) {
                if (s.dimensions.equals(dimensions)) {
                    return s;
                }
            }

            throw new NoSuchElementException("unknown dimensions: " + dimensions);
        }

        private final String className;
        private final String dimensions;
        private final String style;

        Size(String className, String dimensions, String style) {
            this.className = className;
            this.dimensions = dimensions;
            this.style = style;
        }
    }

    static final String ICON_GITLAB = "icon-gitlab";
    static final String ICON_PROJECT = "icon-project";
    static final String ICON_BRANCH = "icon-branch";
    static final String ICON_COMMIT = "icon-commit";
    static final String ICON_MERGE_REQUEST = "icon-merge-request";
    static final String ICON_TAG = "icon-tag";
    private static final String ICON_PATH = "plugin/gitlab-branch-source/images/";

    @Initializer
    public static void initialize() {
        addIcon(ICON_GITLAB);
        addIcon(ICON_PROJECT);
        addIcon(ICON_BRANCH);
        addIcon(ICON_COMMIT);
        addIcon(ICON_MERGE_REQUEST);
        addIcon(ICON_TAG);
    }

    static String iconfilePathPattern(String name) {
        return ICON_PATH + ":size/" + name + ".png";
    }

    static String iconFileName(String name, Size size) {
        Icon icon = icons.getIconByClassSpec(classSpec(name, size));
        if (icon == null) {
            return null;
        }

        JellyContext ctx = new JellyContext();
        ctx.setVariable("resURL", Stapler.getCurrentRequest().getContextPath() + Jenkins.RESOURCE_PATH);
        return icon.getQualifiedUrl(ctx);
    }

    static String avatarFileName(int projectId, String connectionName, Size size) {
        // TODO: size!
        try {
            GitLabProject project = gitLabAPI(connectionName).getProject(projectId);
            String avatarUrl = project.getAvatarUrl();
            if (avatarUrl == null) {
                avatarUrl = groupAvatarUrl(project, connectionName);
            }
            return avatarUrl != null ? avatarUrl : iconFileName(ICON_GITLAB, size);
        } catch (GitLabAPIException e) {
            return iconFileName(ICON_GITLAB, size);
        }

    }

    private static String groupAvatarUrl(GitlabProject project, String connectionName) throws GitLabAPIException {
        GitlabNamespace namespace = project.getNamespace();
        if (namespace.getOwnerId() != null) {
            return null;
        }

        GitLabGroup group = gitLabAPI(connectionName).getGroup(namespace.getId());
        return group.getAvatarUrl();
    }


    private static String classSpec(String name, Size size) {
        return name + " " + size.className;
    }

    private static void addIcon(String name) {
        for (Size size : Size.values()) {
            icons.addIcon(new Icon(classSpec(name, size), ICON_PATH + size.dimensions + "/" + name + ".png", size.style));
        }
    }

    private GitLabSCMIcons() { /* no instances allowed */}
}
