package argelbargel.jenkins.plugins.gitlab_branch_source;

import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabMergeRequest;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.filters.GitLabMergeRequestFilter;
import hudson.model.TaskListener;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMSourceCriteria;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabTag;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabAPI;

class SourceHeads {
    private final int projectId;
    private final SourceSettings settings;

    SourceHeads(int projectId, SourceSettings settings) {
        this.projectId = projectId;
        this.settings = settings;
    }

    void retrieve(@CheckForNull SCMSourceCriteria criteria, @Nonnull SCMHeadObserver observer, @CheckForNull SCMHeadEvent<?> event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        Set<SCMHead> originBranchesWithMergeRequest = new HashSet<>();

        if (settings.originMonitorStrategy().monitored() || settings.forksMonitorStrategy().monitored()) {
            GitLabMergeRequestFilter filter = settings.getMergeRequestFilter();
            for (GitLabMergeRequest mr : filter.filter(gitLabAPI(settings.getConnectionName()).getMergeRequests(projectId))) {
                checkInterrupt();

                GitLabSCMMergeRequest head = new GitLabSCMMergeRequest(mr);
                observer.observe(GitLabSCMHeadLabel.create(head, head.getSource()), head.getCommit());
                if (mr.getSourceProjectId() == projectId) {
                    originBranchesWithMergeRequest.add(head.getSource());
                }
            }
        }

        if (settings.branchMonitorStrategy().monitored()) {
            for (GitlabBranch branch : gitLabAPI(settings.getConnectionName()).getBranches(projectId)) {
                checkInterrupt();

                GitLabSCMBranch head = new GitLabSCMBranch(branch);
                boolean automaticBuild = settings.getBuildBranchesWithMergeRequests() || !originBranchesWithMergeRequest.contains(head);
                observer.observe(GitLabSCMHeadLabel.create(head, automaticBuild), head.getCommit());
            }
        }

        if (settings.tagMonitorStrategy().monitored()) {
            for (GitlabTag tag : gitLabAPI(settings.getConnectionName()).getTags(projectId)) {
                checkInterrupt();

                GitLabSCMTag head = new GitLabSCMTag(tag);
                observer.observe(GitLabSCMHeadLabel.create(head, settings.tagMonitorStrategy().buildUnmerged()), head.getCommit());
            }
        }
    }

    private void checkInterrupt() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }
}
