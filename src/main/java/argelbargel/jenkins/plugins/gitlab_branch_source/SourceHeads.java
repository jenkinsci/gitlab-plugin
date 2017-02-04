package argelbargel.jenkins.plugins.gitlab_branch_source;

import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPI;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPIException;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabMergeRequest;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.filters.GitLabMergeRequestFilter;
import argelbargel.jenkins.plugins.gitlab_branch_source.events.GitLabSCMMergeRequestEvent;
import argelbargel.jenkins.plugins.gitlab_branch_source.events.GitLabSCMPushEvent;
import argelbargel.jenkins.plugins.gitlab_branch_source.events.GitLabSCMTagPushEvent;
import hudson.model.TaskListener;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabAPI;

class SourceHeads {
    static final String ORIGIN_REF_BRANCHES = "refs/heads/";
    static final String ORIGIN_REF_TAGS = "refs/tags/";
    static final String ORIGIN_REF_MERGE_REQUESTS = "refs/merge-requests/";

    private static final SCMHeadObserver NOOP_OBSERVER = new SCMHeadObserver() {
        @Override
        public void observe(@Nonnull SCMHead head, @Nonnull SCMRevision revision) { /* NOOP */ }
    };

    private static GitLabSCMHead createLabel(String pronoun, String originRef, String name, String hash, boolean automaticBuild) {
        return createLabel(new SCMHeadImpl(pronoun, name.replaceFirst("^" + originRef, ""), hash), automaticBuild);
    }

    private static GitLabSCMHead createLabel(GitLabSCMHead head, boolean automaticBuild) {
        return SCMHeadLabel.create(head, automaticBuild);
    }

    private final GitlabProject project;
    private final SourceSettings settings;
    private transient Collection<String> branchesWithMergeRequestsCache;

    SourceHeads(GitlabProject project, SourceSettings settings) {
        this.project = project;
        this.settings = settings;
    }

    GitLabSCMHead createBranch(String name, String hash) throws IOException, InterruptedException {
        boolean automaticBuild = true;

        if (!settings.getBuildBranchesWithMergeRequests()) {
            if (branchesWithMergeRequestsCache == null) {
                retrieveMergeRequests(NOOP_OBSERVER, TaskListener.NULL);
            }

            automaticBuild = !branchesWithMergeRequestsCache.contains(name.replaceFirst(ORIGIN_REF_BRANCHES, ""));
        }

        return createBranch(name, hash, automaticBuild);
    }

    @SuppressWarnings("SameParameterValue")
    GitLabSCMHead createBranch(String name, boolean automaticBuild) {
        return createBranch(name, "HEAD", automaticBuild);
    }

    GitLabSCMHead createBranch(String name, String hash, boolean automaticBuild) {
        return createLabel(Messages.GitLabSCMBranch_Pronoun(), ORIGIN_REF_BRANCHES, name, hash, automaticBuild);
    }

    GitLabSCMHead createTag(String name, String hash) {
        return createLabel(Messages.GitLabSCMTag_Pronoun(), ORIGIN_REF_TAGS, name, hash, settings.tagMonitorStrategy().buildUnmerged());
    }

    GitLabSCMHead createMergeRequest(String name, GitLabSCMHead source, GitLabSCMHead target) {
        return createLabel(new SCMMergeRequestHead(name.replaceFirst("^" + ORIGIN_REF_MERGE_REQUESTS, ""), source, target), true);
    }

    void retrieve(@CheckForNull SCMSourceCriteria criteria, @Nonnull SCMHeadObserver observer, @Nonnull SCMHeadEvent<?> event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        if (event instanceof GitLabSCMMergeRequestEvent) {
            retrieveMergeRequest(criteria, observer, (GitLabSCMMergeRequestEvent) event, listener);
        } else if (event instanceof GitLabSCMTagPushEvent) {
            retrieveTag(criteria, observer, (GitLabSCMTagPushEvent) event, listener);
        } else if (event instanceof GitLabSCMPushEvent) {
            retrieveBranch(criteria, observer, (GitLabSCMPushEvent) event, listener);
        } else {
            retrieveAll(criteria, observer, listener);
        }
    }

    private void retrieveMergeRequest(@CheckForNull SCMSourceCriteria criteria, @Nonnull SCMHeadObserver observer, @Nonnull GitLabSCMMergeRequestEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
          int mrId = event.getPayload().getObjectAttributes().getId();
          listener.getLogger().format(Messages.GitLabSCMSource_retrievingMergeRequest(mrId) + "\n");
          try {
              GitLabMergeRequest mr = api().getMergeRequest(project.getId(), mrId);
              listener.getLogger().format(Messages.GitLabSCMSource_monitoringMergeRequest(mr.getId()) + "\n");
              observe(observer, mr);
          } catch (NoSuchElementException e) {
              listener.getLogger().format(Messages.GitLabSCMSource_removedMergeRequest(mrId) + "\n");
          }
    }

    private void retrieveBranch(@CheckForNull SCMSourceCriteria criteria, @Nonnull SCMHeadObserver observer, @Nonnull GitLabSCMPushEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        String branchName = event.getPayload().getRef().replaceFirst(ORIGIN_REF_BRANCHES, "");
        listener.getLogger().format(Messages.GitLabSCMSource_retrievingBranch(branchName) + "\n");
        try {
            GitlabBranch branch = api().getBranch(project.getId(), branchName);
            listener.getLogger().format(Messages.GitLabSCMSource_monitoringBranch(branch.getName()) + "\n");
            observe(observer, branch);
        } catch (NoSuchElementException e) {
            listener.getLogger().format(Messages.GitLabSCMSource_removedHead(branchName) + "\n");
        }
    }

    private void retrieveTag(@CheckForNull SCMSourceCriteria criteria, @Nonnull SCMHeadObserver observer, @Nonnull GitLabSCMTagPushEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        String tagName = event.getPayload().getRef().replaceFirst(ORIGIN_REF_TAGS, "");
        listener.getLogger().format(Messages.GitLabSCMSource_retrievingTag(tagName) + "\n");
        try {
            GitlabTag tag = api().getTag(project.getId(), tagName);
            listener.getLogger().format(Messages.GitLabSCMSource_monitoringTag(tag.getName()) + "\n");
            observe(observer, tag);
        } catch (NoSuchElementException e) {
            listener.getLogger().format(Messages.GitLabSCMSource_removedHead(tagName) + "\n");
        }
    }


    private void retrieveAll(@CheckForNull SCMSourceCriteria criteria, @Nonnull SCMHeadObserver observer, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().format(Messages.GitLabSCMSource_retrievingHeadsForProject(project.getPathWithNamespace()) + "\n");
        retrieveMergeRequests(observer, listener);

        if (settings.branchMonitorStrategy().monitored()) {
            listener.getLogger().format(Messages.GitLabSCMSource_retrievingBranches() + "\n");

            for (GitlabBranch branch : api().getBranches(project.getId())) {
                checkInterrupt();
                listener.getLogger().format(Messages.GitLabSCMSource_monitoringBranch(branch.getName()) + "\n");
                observe(observer, branch);
            }
        }

        if (settings.tagMonitorStrategy().monitored()) {
            listener.getLogger().format(Messages.GitLabSCMSource_retrievingTags() + "\n");
            for (GitlabTag tag : api().getTags(project.getId())) {
                checkInterrupt();
                listener.getLogger().format(Messages.GitLabSCMSource_monitoringTag(tag.getName()) + "\n");
                observe(observer, tag);
            }
        }
    }

    private void retrieveMergeRequests(@Nonnull SCMHeadObserver observer, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        branchesWithMergeRequestsCache = new HashSet<>();

        if (settings.originMonitorStrategy().monitored() || settings.forksMonitorStrategy().monitored()) {
            listener.getLogger().format(Messages.GitLabSCMSource_retrievingMergeRequests() + "\n");

            GitLabMergeRequestFilter filter = settings.getMergeRequestFilter(listener);
            for (GitLabMergeRequest mr : filter.filter(api().getMergeRequests(project.getId()))) {
                checkInterrupt();

                observe(observer, mr);
                listener.getLogger().format(Messages.GitLabSCMSource_monitoringMergeRequest(mr.getId()) + "\n");
                if (Objects.equals(mr.getSourceProjectId(), project.getId())) {
                    branchesWithMergeRequestsCache.add(mr.getSourceBranch());
                }
            }
        }
    }

    private void observe(@Nonnull SCMHeadObserver observer, GitlabBranch branch) throws IOException, InterruptedException {
        observe(observer, createBranch(branch.getName(), branch.getCommit().getId()));
    }

    private void observe(@Nonnull SCMHeadObserver observer, GitlabTag tag) {
        observe(observer, createTag(tag.getName(), tag.getCommit().getId()));
    }

    private void observe(@Nonnull SCMHeadObserver observer, GitLabMergeRequest mergeRequest) throws IOException, InterruptedException {
        observe(observer,
                createMergeRequest(
                        String.valueOf(mergeRequest.getId()),
                        createBranch(mergeRequest.getSourceBranch(), mergeRequest.getSha(), true),
                        createBranch(mergeRequest.getTargetBranch(), true)));
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
