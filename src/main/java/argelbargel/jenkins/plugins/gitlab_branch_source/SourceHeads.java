package argelbargel.jenkins.plugins.gitlab_branch_source;

import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPI;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPIException;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabMergeRequest;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.filters.GitLabMergeRequestFilter;
import argelbargel.jenkins.plugins.gitlab_branch_source.events.GitLabSCMMergeRequestEvent;
import argelbargel.jenkins.plugins.gitlab_branch_source.events.GitLabSCMPushEvent;
import argelbargel.jenkins.plugins.gitlab_branch_source.events.GitLabSCMTagPushEvent;
import hudson.model.TaskListener;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSourceCriteria;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabTag;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabAPI;
import static argelbargel.jenkins.plugins.gitlab_branch_source.HeadLabel.createLabel;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead.ORIGIN_REF_BRANCHES;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead.ORIGIN_REF_TAGS;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead.createBranch;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead.createMergeRequest;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead.createTag;
import static hudson.model.TaskListener.NULL;
import static java.util.Collections.emptyMap;

class SourceHeads {
    private static final SCMHeadObserver NOOP_OBSERVER = new SCMHeadObserver() {
        @Override
        public void observe(@Nonnull jenkins.scm.api.SCMHead head, @Nonnull SCMRevision revision) { /* NOOP */ }
    };
    private static final SCMSourceCriteria ALL_CRITERIA = new SCMSourceCriteria() {
        @Override
        public boolean isHead(@Nonnull Probe probe, @Nonnull TaskListener taskListener) throws IOException {
            return true;
        }
    };

    private final GitlabProject project;
    private final SourceSettings settings;
    private transient Map<Integer, String> branchesWithMergeRequestsCache;

    SourceHeads(GitlabProject project, SourceSettings settings) {
        this.project = project;
        this.settings = settings;
    }

    private Map<Integer, String> getBranchesWithMergeRequests(TaskListener listener) throws IOException, InterruptedException {
        if (settings.getBuildBranchesWithMergeRequests()) {
            return emptyMap();
        }

        if (branchesWithMergeRequestsCache == null) {
            retrieveMergeRequests(ALL_CRITERIA, NOOP_OBSERVER, listener);
        }

        return branchesWithMergeRequestsCache;
    }

    void retrieve(@CheckForNull SCMSourceCriteria criteria, @Nonnull SCMHeadObserver observer, @Nonnull SCMHeadEvent<?> event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        if (event instanceof GitLabSCMMergeRequestEvent) {
            retrieveMergeRequest(observer, (GitLabSCMMergeRequestEvent) event, listener);
        } else if (event instanceof GitLabSCMTagPushEvent) {
            retrieveTag(observer, (GitLabSCMTagPushEvent) event, listener);
        } else if (event instanceof GitLabSCMPushEvent) {
            retrieveBranch(observer, (GitLabSCMPushEvent) event, listener);
        } else {
            retrieveAll(criteria, observer, listener);
        }
    }

    private void retrieveMergeRequest(@Nonnull SCMHeadObserver observer, @Nonnull GitLabSCMMergeRequestEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        int mrId = event.getPayload().getObjectAttributes().getId();
        listener.getLogger().format(Messages.GitLabSCMSource_retrievingMergeRequest(mrId) + "\n");
        try {
            GitLabMergeRequest mr = api().getMergeRequest(project.getId(), mrId);
            observe(mr, observer, listener);
        } catch (NoSuchElementException e) {
            listener.getLogger().format(Messages.GitLabSCMSource_removedMergeRequest(mrId) + "\n");
            getBranchesWithMergeRequests(listener).remove(mrId);
        }
    }

    private void retrieveBranch(@Nonnull SCMHeadObserver observer, @Nonnull GitLabSCMPushEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        String branchName = event.getPayload().getRef().replaceFirst(ORIGIN_REF_BRANCHES, "");
        listener.getLogger().format(Messages.GitLabSCMSource_retrievingBranch(branchName) + "\n");
        try {
            GitlabBranch branch = api().getBranch(project.getId(), branchName);
            observe(branch, observer, listener);
        } catch (NoSuchElementException e) {
            listener.getLogger().format(Messages.GitLabSCMSource_removedHead(branchName) + "\n");
        }
    }

    private void retrieveTag(@Nonnull SCMHeadObserver observer, @Nonnull GitLabSCMTagPushEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        String tagName = event.getPayload().getRef().replaceFirst(ORIGIN_REF_TAGS, "");
        listener.getLogger().format(Messages.GitLabSCMSource_retrievingTag(tagName) + "\n");
        try {
            GitlabTag tag = api().getTag(project.getId(), tagName);
            tag.getCommit().getCommittedDate().getTime();
            observe(tag, observer, listener);
        } catch (NoSuchElementException e) {
            listener.getLogger().format(Messages.GitLabSCMSource_removedHead(tagName) + "\n");
        }
    }

    private void retrieveAll(@CheckForNull SCMSourceCriteria criteria, @Nonnull SCMHeadObserver observer, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().format(Messages.GitLabSCMSource_retrievingHeadsForProject(project.getPathWithNamespace()) + "\n");
        retrieveMergeRequests(criteria, observer, listener);
        retrieveBranches(criteria, observer, listener);
        retrieveTags(criteria, observer, listener);
    }

    private void retrieveMergeRequests(@CheckForNull SCMSourceCriteria criteria, @Nonnull SCMHeadObserver observer, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        branchesWithMergeRequestsCache = new HashMap<>();

        if (settings.originMonitorStrategy().monitored() || settings.forksMonitorStrategy().monitored()) {
            listener.getLogger().format(Messages.GitLabSCMSource_retrievingMergeRequests() + "\n");

            GitLabMergeRequestFilter filter = settings.getMergeRequestFilter(listener);
            for (GitLabMergeRequest mr : filter.filter(api().getMergeRequests(project.getId()))) {
                checkInterrupt();
                observe(mr, observer, listener);
            }
        }
    }

    private void retrieveBranches(@CheckForNull SCMSourceCriteria criteria, @Nonnull SCMHeadObserver observer, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        if (settings.branchMonitorStrategy().monitored()) {
            listener.getLogger().format(Messages.GitLabSCMSource_retrievingBranches() + "\n");

            for (GitlabBranch branch : api().getBranches(project.getId())) {
                checkInterrupt();
                observe(branch, observer, listener);
            }
        }
    }

    private void retrieveTags(@CheckForNull SCMSourceCriteria criteria, @Nonnull SCMHeadObserver observer, @Nonnull TaskListener listener) throws GitLabAPIException, InterruptedException {
        if (settings.tagMonitorStrategy().monitored()) {
            listener.getLogger().format(Messages.GitLabSCMSource_retrievingTags() + "\n");
            for (GitlabTag tag : api().getTags(project.getId())) {
                checkInterrupt();
                observe(tag, observer, listener);
            }
        }
    }

    private void observe(GitlabBranch branch, @Nonnull SCMHeadObserver observer, TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().format(Messages.GitLabSCMSource_monitoringBranch(branch.getName()) + "\n");
        observe(observer, createLabel(
                createBranch(
                        branch.getName(),
                        branch.getCommit().getId(),
                        getBranchesWithMergeRequests(NULL).containsValue(branch.getName()))));
    }

    private void observe(GitlabTag tag, @Nonnull SCMHeadObserver observer, TaskListener listener) {
        listener.getLogger().format(Messages.GitLabSCMSource_monitoringTag(tag.getName()) + "\n");
        observe(observer,
                createLabel(
                        createTag(
                                tag.getName(),
                                tag.getCommit().getId(),
                                tag.getCommit().getCommittedDate().getTime()),
                        settings.tagMonitorStrategy().buildUnmerged()));
    }

    private void observe(GitLabMergeRequest mergeRequest, @Nonnull SCMHeadObserver observer, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().format(Messages.GitLabSCMSource_monitoringMergeRequest(mergeRequest.getId()) + "\n");
        observe(observer,
                createLabel(
                        createMergeRequest(
                                mergeRequest.getId(), mergeRequest.getTitle(),
                                createBranch(mergeRequest.getSourceBranch(), mergeRequest.getSha()),
                                createBranch(mergeRequest.getTargetBranch()))));

        if (Objects.equals(mergeRequest.getSourceProjectId(), project.getId())) {
            getBranchesWithMergeRequests(listener).put(mergeRequest.getId(), mergeRequest.getSourceBranch());
        }
    }

    private void observe(@Nonnull SCMHeadObserver observer, GitLabSCMHead head) {
        observer.observe(head, head.getRevision());
    }

    private GitLabAPI api() throws GitLabAPIException {
        return gitLabAPI(settings.getConnectionName());
    }

    private void checkInterrupt() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }
}
