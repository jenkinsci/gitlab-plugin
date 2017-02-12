package argelbargel.jenkins.plugins.gitlab_branch_source;


import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPI;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPIException;
import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMProbe;
import jenkins.scm.api.SCMProbeStat;
import jenkins.scm.api.SCMRevision;
import org.apache.commons.lang.StringUtils;
import org.gitlab.api.models.GitlabRepositoryTree;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabAPI;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead.REVISION_HEAD;
import static java.util.logging.Level.SEVERE;


class GitLabSCMProbe extends SCMProbe {
    private static final String TYPE_BLOB = "blob";
    private static final String TYPE_DIRECTORY = "tree";
    private static final Logger LOGGER = Logger.getLogger(GitLabSCMProbe.class.getName());


    static GitLabSCMProbe create(GitLabSCMSource source, SCMHead head, SCMRevision revision) {
        if (!SCMRevisionImpl.class.isInstance(revision)) {
            return create(source, head, new SCMRevisionImpl(head, REVISION_HEAD));
        }

        if (head instanceof GitLabSCMMergeRequestHead) {
            return create(source, ((GitLabSCMMergeRequestHead) head).getSource(), revision);
        }

        int projectId = (head instanceof GitLabSCMHead) ? ((GitLabSCMHead) head).getProjectId() : source.getProjectId();
        return new GitLabSCMProbe(source.getConnectionName(), projectId, head.getName(), ((SCMRevisionImpl) revision).getHash());
    }


    private final String connectionName;
    private final int projectId;
    private final String name;
    private final String hash;


    private GitLabSCMProbe(String connectionName, int projectId, String name, String hash) {
        this.connectionName = connectionName;
        this.projectId = projectId;
        this.name = name;
        this.hash = StringUtils.isBlank(hash) || REVISION_HEAD.equals(hash) ? name : hash;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public long lastModified() {
        try {
            return api().getCommit(projectId, hash).getCreatedAt().getTime();
        } catch (GitLabAPIException e) {
            LOGGER.log(SEVERE, "could not get last modification time for project " + projectId + ", branch " + name + " (" + hash + ")", e);
            return 0L;
        }
    }

    @Nonnull
    @Override
    public SCMProbeStat stat(@Nonnull String path) throws IOException {
        try {
            int index = path.lastIndexOf('/') + 1;
            return stat(path.substring(0, index), path.substring(index));
        } catch (FileNotFoundException fnf) {
            return SCMProbeStat.fromType(SCMFile.Type.NONEXISTENT);
        }
    }

    @Nonnull
    private SCMProbeStat stat(String root, String name) throws GitLabAPIException, FileNotFoundException {
        for (GitlabRepositoryTree content : api().getTree(projectId, hash, root)) {
            if (content.getName().equals(name)) {
                if (TYPE_BLOB.equals(content.getType())) {
                    return SCMProbeStat.fromType(SCMFile.Type.REGULAR_FILE);
                } else if (TYPE_DIRECTORY.equals(content.getType())) {
                    return SCMProbeStat.fromType(SCMFile.Type.DIRECTORY);
                } else {
                    return SCMProbeStat.fromType(SCMFile.Type.OTHER);
                }
            }
        }

        throw new FileNotFoundException(root + "/" + name);
    }

    @Override
    public void close() throws IOException { /* NOOP */ }

    private GitLabAPI api() throws GitLabAPIException {
        return gitLabAPI(connectionName);
    }
}
