package argelbargel.jenkins.plugins.gitlab_branch_source;


import org.eclipse.jgit.transport.RefSpec;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


enum GitLabSCMRefSpec {
    BRANCHES(new RefSpec("+refs/heads/*:refs/remotes/origin/*")),
    TAGS(new RefSpec("+refs/tags/*:refs/remotes/origin/tags/*")),
    MERGE_REQUESTS(new RefSpec("+refs/merge-requests/*/head:refs/remotes/origin/merge-requests/*"));

    private final RefSpec delegate;
    private final Pattern sourceNamePattern;

    GitLabSCMRefSpec(RefSpec refSpec) {
        this.delegate = refSpec;
        this.sourceNamePattern = Pattern.compile(replaceWildcard(delegate.getSource(), "(.*)"));
    }

    RefSpec delegate() {
        return delegate;
    }

    String remoteName(String path) {
        Matcher m = sourceNamePattern.matcher(path);
        if (m.matches()) {
            return m.group(1);
        }

        return path;
    }

    String sourceRef(String name) {
        return replaceWildcard(delegate.getSource(), name);
    }

    String destinationRef(String name) {
        return replaceWildcard(delegate.getDestination(), name);
    }

    private String replaceWildcard(String refspec, String replacement) {
        return refspec.replaceFirst("\\*", replacement);
    }
}
