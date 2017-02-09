package argelbargel.jenkins.plugins.gitlab_branch_source;


import org.eclipse.jgit.transport.RefSpec;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


enum GitLabSCMRefSpec {
    BRANCHES(new RefSpec("+refs/heads*:refs/remotes/origin/*")),
    TAGS(new RefSpec("+refs/tags/*:refs/remotes/origin/tags/*")),
    MERGE_REQUESTS(new RefSpec("+refs/merge-requests/*/head:refs/remotes/origin/merge-requests/*"));

    private final RefSpec refSpec;
    private final Pattern pattern;

    GitLabSCMRefSpec(RefSpec refSpec) {
        this.refSpec = refSpec;
        this.pattern = Pattern.compile(refSpec.getSource().replaceFirst("^\\+", "").replaceFirst("\\*", "(.*)"));
    }

    RefSpec refSpec() {
        return refSpec;
    }

    String name(String path) {
        Matcher m = pattern.matcher(path);
        if (m.matches()) {
            return m.group(1);
        }

        return path;
    }
}
