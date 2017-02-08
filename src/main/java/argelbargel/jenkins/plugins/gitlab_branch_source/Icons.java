package argelbargel.jenkins.plugins.gitlab_branch_source;

import jenkins.model.Jenkins;
import org.apache.commons.jelly.JellyContext;
import org.jenkins.ui.icon.Icon;

import static org.jenkins.ui.icon.Icon.ICON_LARGE_STYLE;
import static org.jenkins.ui.icon.Icon.ICON_MEDIUM_STYLE;
import static org.jenkins.ui.icon.Icon.ICON_SMALL_STYLE;
import static org.jenkins.ui.icon.Icon.ICON_XLARGE_STYLE;
import static org.jenkins.ui.icon.IconSet.icons;

final class Icons {
    enum Size {
        SMALL("icon-sm", "16x16", ICON_SMALL_STYLE),
        MEDIUM("icon-md", "24x24", ICON_MEDIUM_STYLE),
        LARGE("icon-lg", "32x32", ICON_LARGE_STYLE),
        XLARGE("icon-xlg", "48x48", ICON_XLARGE_STYLE);

        private final String className;
        private final String dimensions;
        private final String style;

        Size(String className, String dimensions, String style) {
            this.className = className;
            this.dimensions = dimensions;
            this.style = style;
        }
    }

    static final String ICON_GITLAB_LOGO = "icon-gitlab-logo";
    private static final String ICON_PATH = "plugin/gitlab-branch-source/images/";

    static String iconfilePathPattern(String name) {
        return ICON_PATH + ":size/" + name + ".png";
    }

    static String iconFileName(String name, Size size) {
        Icon icon = icons.getIconByClassSpec(classSpec(name, size));
        if (icon == null) {
            return null;
        }

        JellyContext ctx = new JellyContext();
        ctx.setVariable("resURL", Jenkins.RESOURCE_PATH);
        return icon.getQualifiedUrl(ctx);
    }

    static void initialize() {
        addIcon(ICON_GITLAB_LOGO);
    }

    private static String classSpec(String name, Size size) {
        return name + " " + size.className;
    }

    private static void addIcon(String name) {
        for (Size size : Size.values()) {
            icons.addIcon(new Icon(classSpec(name, size), ICON_PATH + size.dimensions + "/" + name + ".png", size.style));
        }
    }

    private Icons() { /* no instances allowed */}
}
