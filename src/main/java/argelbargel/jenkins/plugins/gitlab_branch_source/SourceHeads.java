package argelbargel.jenkins.plugins.gitlab_branch_source;


import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPI;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPIException;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabMergeRequest;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.filters.GitLabMergeRequestFilter;
import argelbargel.jenkins.plugins.gitlab_branch_source.events.GitLabSCMMergeRequestEvent;
import argelbargel.jenkins.plugins.gitlab_branch_source.events.GitLabSCMPushEvent;
import argelbargel.jenkins.plugins.gitlab_branch_source.events.GitLabSCMTagPushEvent;
import hudson.model.TaskListener;
import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.scm.api.SCMHead;
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
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead.createBranch;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead.createMergeRequest;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead.createTag;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMMergeRequestHead.CAN_BE_MERGED;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMRefSpec.BRANCHES;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMRefSpec.TAGS;
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

    @CheckForNull
    SCMRevision retrieve(@Nonnull SCMHead head, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        log(listener, Messages.GitLabSCMSource_retrievingRevision(head.getName()));
        try {
            return new SCMRevisionImpl(head, retrieveRevision(head));
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private String retrieveRevision(SCMHead head) throws GitLabAPIException {
        if (head instanceof GitLabSCMMergeRequestHead) {
            return retrieveMergeRequestRevision(((GitLabSCMMergeRequestHead) head).getId());
        } else if (head instanceof GitLabSCMTagHead) {
            return retrieveTagRevision(head.getName());
        }

        return retrieveBranchRevision(head.getName());
    }

    private String retrieveMergeRequestRevision(String id) throws GitLabAPIException {
        return api().getMergeRequest(project.getId(), id).getSha();
    }

    private String retrieveTagRevision(String name) throws GitLabAPIException {
        return api().getTag(project.getId(), name).getCommit().getId();
    }

    private String retrieveBranchRevision(String name) throws GitLabAPIException {
        return api().getBranch(project.getId(), name).getCommit().getId();
    }

    private void retrieveMergeRequest(@Nonnull SCMHeadObserver observer, @Nonnull GitLabSCMMergeRequestEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        int mrId = event.getPayload().getObjectAttributes().getId();
        log(listener, Messages.GitLabSCMSource_retrievingMergeRequest(mrId));
        try {
            GitLabMergeRequest mr = api().getMergeRequest(project.getId(), mrId);
            observe(mr, observer, listener);
        } catch (NoSuchElementException e) {
            log(listener, Messages.GitLabSCMSource_removedMergeRequest(mrId));
            branchesWithMergeRequests(listener).remove(mrId);
        }
    }

    private void retrieveBranch(@Nonnull SCMHeadObserver observer, @Nonnull GitLabSCMPushEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        String branchName = BRANCHES.remoteName(event.getPayload().getRef());
        log(listener, Messages.GitLabSCMSource_retrievingBranch(branchName));
        try {
            GitlabBranch branch = api().getBranch(project.getId(), branchName);
            observe(branch, observer, listener);
        } catch (NoSuchElementException e) {
            log(listener, Messages.GitLabSCMSource_removedHead(branchName));
        }
    }

    private void retrieveTag(@Nonnull SCMHeadObserver observer, @Nonnull GitLabSCMTagPushEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        String tagName = TAGS.remoteName(event.getPayload().getRef());
        log(listener, Messages.GitLabSCMSource_retrievingTag(tagName));
        try {
            GitlabTag tag = api().getTag(project.getId(), tagName);
            tag.getCommit().getCommittedDate().getTime();
            observe(tag, observer, listener);
        } catch (NoSuchElementException e) {
            log(listener, Messages.GitLabSCMSource_removedHead(tagName));
        }
    }

    private void retrieveAll(@CheckForNull SCMSourceCriteria criteria, @Nonnull SCMHeadObserver observer, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        log(listener, Messages.GitLabSCMSource_retrievingHeadsForProject(project.getPathWithNamespace()));
        retrieveMergeRequests(criteria, observer, listener);
        retrieveBranches(criteria, observer, listener);
        retrieveTags(criteria, observer, listener);
    }

    private void retrieveMergeRequests(@CheckForNull SCMSourceCriteria criteria, @Nonnull SCMHeadObserver observer, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        branchesWithMergeRequestsCache = new HashMap<>();

        if (settings.originMonitorStrategy().monitored() || settings.forksMonitorStrategy().monitored()) {
            log(listener, Messages.GitLabSCMSource_retrievingMergeRequests());

            GitLabMergeRequestFilter filter = settings.getMergeRequestFilter(listener);
            for (GitLabMergeRequest mr : filter.filter(api().getMergeRequests(project.getId()))) {
                checkInterrupt();
                observe(mr, observer, listener);
            }
        }
    }

    private void retrieveBranches(@CheckForNull SCMSourceCriteria criteria, @Nonnull SCMHeadObserver observer, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        if (settings.branchMonitorStrategy().monitored()) {
            log(listener, Messages.GitLabSCMSource_retrievingBranches());

            for (GitlabBranch branch : api().getBranches(project.getId())) {
                checkInterrupt();
                observe(branch, observer, listener);
            }
        }
    }

    private void retrieveTags(@CheckForNull SCMSourceCriteria criteria, @Nonnull SCMHeadObserver observer, @Nonnull TaskListener listener) throws GitLabAPIException, InterruptedException {
        if (settings.tagMonitorStrategy().monitored()) {
            log(listener, Messages.GitLabSCMSource_retrievingTags());
            for (GitlabTag tag : api().getTags(project.getId())) {
                checkInterrupt();
                observe(tag, observer, listener);
            }
        }
    }

    private void observe(GitlabBranch branch, @Nonnull SCMHeadObserver observer, TaskListener listener) throws IOException, InterruptedException {
        log(listener, Messages.GitLabSCMSource_monitoringBranch(branch.getName()));

        boolean hasMergeRequest = branchesWithMergeRequests(NULL).containsValue(branch.getName());
        if (hasMergeRequest && !settings.getBuildBranchesWithMergeRequests()) {
            log(listener, Messages.GitLabSCMSource_willNotBuildBranchWithMergeRequest(branch.getName()));
        }

        observe(observer, createBranch(project.getId(), branch.getName(), branch.getCommit().getId(), hasMergeRequest));
    }

    private void observe(GitlabTag tag, @Nonnull SCMHeadObserver observer, TaskListener listener) {
        log(listener, Messages.GitLabSCMSource_monitoringTag(tag.getName()));
        observe(observer, createTag(project.getId(), tag.getName(), tag.getCommit().getId(), tag.getCommit().getCommittedDate().getTime()));
    }

    private void observe(GitLabMergeRequest mergeRequest, @Nonnull SCMHeadObserver observer, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        log(listener, Messages.GitLabSCMSource_monitoringMergeRequest(mergeRequest.getId()));

        String targetBranch = mergeRequest.getTargetBranch();
        GitLabSCMMergeRequestHead head = createMergeRequest(
                mergeRequest.getId(), mergeRequest.getTitle(),
                createBranch(mergeRequest.getSourceProjectId(), mergeRequest.getSourceBranch(), mergeRequest.getSha()),
                createBranch(mergeRequest.getTargetProjectId(), targetBranch, retrieveBranchRevision(targetBranch)),
                Objects.equals(mergeRequest.getMergeStatus(), CAN_BE_MERGED));

        // TOOD: We're doing these checks at least three times - centralize in GitLabMergeRequest?
        boolean fromOrigin = Objects.equals(mergeRequest.getSourceProjectId(), project.getId());

        if ((fromOrigin && settings.originMonitorStrategy().buildUnmerged()) || (!fromOrigin && settings.forksMonitorStrategy().buildUnmerged())) {
            observe(observer, head);
        }

        if ((fromOrigin && settings.originMonitorStrategy().buildMerged()) || (!fromOrigin && settings.forksMonitorStrategy().buildMerged())) {
            if (!head.isMergeable() && ((fromOrigin && settings.originMonitorStrategy().buildOnlyMergeableRequestsMerged()) || (!fromOrigin && settings.forksMonitorStrategy().buildOnlyMergeableRequestsMerged()))) {
                log(listener, Messages.GitLabSCMSource_willNotBuildUnmergeableRequest(mergeRequest.getId(), mergeRequest.getTargetBranch(), mergeRequest.getMergeStatus()));
            }
            observe(observer, head.merged());
        }

        if (fromOrigin) {
            branchesWithMergeRequests(listener).put(mergeRequest.getId(), mergeRequest.getSourceBranch());
        }
    }

    private void observe(@Nonnull SCMHeadObserver observer, GitLabSCMHead head) {
        observer.observe(head, head.getRevision());
    }

    private GitLabAPI api() throws GitLabAPIException {
        return gitLabAPI(settings.getConnectionName());
    }

    private void log(@Nonnull TaskListener listener, String message) {
        listener.getLogger().format(message + "\n");
    }

    private Map<Integer, String> branchesWithMergeRequests(TaskListener listener) throws IOException, InterruptedException {
        if (settings.getBuildBranchesWithMergeRequests()) {
            return emptyMap();
        }

        if (branchesWithMergeRequestsCache == null) {
            retrieveMergeRequests(ALL_CRITERIA, NOOP_OBSERVER, listener);
        }

        return branchesWithMergeRequestsCache;
    }

    private void checkInterrupt() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }
}
