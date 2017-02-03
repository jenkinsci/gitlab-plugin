package argelbargel.jenkins.plugins.gitlab_branch_source;

import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPI;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPIException;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabMergeRequest;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.filters.GitLabMergeRequestFilter;
import argelbargel.jenkins.plugins.gitlab_branch_source.events.GitLabSCMPushEvent;
import argelbargel.jenkins.plugins.gitlab_branch_source.events.GitLabSCMTagPushEvent;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import hudson.model.TaskListener;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMSourceCriteria;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabTag;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabAPI;

public class GitLabSCMHeads {
    static final String ORIGIN_REF_BRANCHES = "refs/heads/";
    static final String ORIGIN_REF_TAGS = "refs/tags/";
    static final String ORIGIN_REF_MERGE_REQUESTS = "refs/merge-requests/";


    public static GitLabSCMHead createMergeRequest(String name, GitLabSCMHead source, GitLabSCMHead target) {
        return createLabel(new GitLabSCMMergeRequest(name.replaceFirst("^" + ORIGIN_REF_MERGE_REQUESTS, ""), source, target), true);
    }

    public static GitLabSCMHead createBranch(String name) {
        return createBranch(name, "HEAD");
    }

    public static GitLabSCMHead createBranch(String name, String hash) {
        return createBranch(name, hash, true);
    }

    public static GitLabSCMHead createBranch(String name, String hash, boolean automaticBuild) {
        return createLabel(Messages.GitLabSCMBranch_Pronoun(), ORIGIN_REF_BRANCHES, name, hash, automaticBuild);
    }

    public static GitLabSCMHead createTag(String name, String hash) {
        return createTag(name, hash, false);
    }

    public static GitLabSCMHead createTag(String name, String hash, boolean automaticBuild) {
        return createLabel(Messages.GitLabSCMTag_Pronoun(), ORIGIN_REF_TAGS, name, hash, automaticBuild);
    }

    private static GitLabSCMHead createLabel(String pronoun, String originRef, String name, String hash, boolean automaticBuild) {
        return createLabel(new GitLabSCMHeadImpl(pronoun, name.replaceFirst("^" + originRef, ""), hash), automaticBuild);
    }

    private static GitLabSCMHead createLabel(GitLabSCMHead head, boolean automaticBuild) {
        return GitLabSCMHeadLabel.create(head, automaticBuild);
    }


    private final int projectId;
    private final SourceSettings settings;

    GitLabSCMHeads(int projectId, SourceSettings settings) {
        this.projectId = projectId;
        this.settings = settings;
    }

    void retrieve(@CheckForNull SCMSourceCriteria criteria, @Nonnull SCMHeadObserver observer, @CheckForNull SCMHeadEvent<?> event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        if (event instanceof GitLabSCMTagPushEvent) {
            retrieveTag(observer, ((GitLabSCMPushEvent) event).getPayload(), listener);
        } else if (event instanceof GitLabSCMPushEvent) {
            retrieveBranch(observer, ((GitLabSCMPushEvent) event).getPayload(), listener);
        } else {
            retrieveAll(criteria, observer, listener);
        }
    }

    private void retrieveAll(@CheckForNull SCMSourceCriteria criteria, @Nonnull SCMHeadObserver observer, @Nonnull TaskListener listener) throws GitLabAPIException, InterruptedException {
        Set<String> originBranchesWithMergeRequest = retrieveMergeRequests(observer, listener);

        if (settings.branchMonitorStrategy().monitored()) {
            for (GitlabBranch branch : api().getBranches(projectId)) {
                checkInterrupt();

                observe(observer, branch, settings.getBuildBranchesWithMergeRequests() || !originBranchesWithMergeRequest.contains(branch.getName()));
            }
        }

        if (settings.tagMonitorStrategy().monitored()) {
            for (GitlabTag tag : api().getTags(projectId)) {
                checkInterrupt();

                observe(observer, tag);
            }
        }
    }

    private Set<String> retrieveMergeRequests(@Nonnull SCMHeadObserver observer, @Nonnull TaskListener listener) throws GitLabAPIException, InterruptedException {
        Set<String> branchesWithMergeRequest = new HashSet<>();

        if (settings.originMonitorStrategy().monitored() || settings.forksMonitorStrategy().monitored()) {
            GitLabMergeRequestFilter filter = settings.getMergeRequestFilter();
            for (GitLabMergeRequest mr : filter.filter(api().getMergeRequests(projectId))) {
                checkInterrupt();

                observe(observer, mr);
                if (mr.getSourceProjectId() == projectId) {
                    branchesWithMergeRequest.add(mr.getSourceBranch());
                }
            }
        }

        return branchesWithMergeRequest;
    }

    private void retrieveBranch(@Nonnull SCMHeadObserver observer, @CheckForNull PushHook hook, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        try {
            GitlabBranch branch = api().getBranch(projectId, hook.getRef().replace(ORIGIN_REF_BRANCHES, ""));
            boolean automaticBuild = settings.getBuildBranchesWithMergeRequests() || !retrieveMergeRequests(observer, listener).contains(branch.getName());
            observe(observer, branch, automaticBuild);
        } catch (NoSuchElementException e) {
            listener.getLogger().format("ref %s does not exist in repository any more", hook.getRef());
        }
    }

    private void retrieveTag(@Nonnull SCMHeadObserver observer, @CheckForNull PushHook hook, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        try {
            GitlabTag tag = api().getTag(projectId, hook.getRef().replace(ORIGIN_REF_TAGS, ""));
            observe(observer, tag);
        } catch (NoSuchElementException e) {
            listener.getLogger().format("ref %s does not exist in repository any more", hook.getRef());
        }
    }

    private void observe(@Nonnull SCMHeadObserver observer, GitlabBranch branch, boolean automaticBuild) {
        observe(observer, createBranch(branch.getName(), branch.getCommit().getId(), automaticBuild));
    }

    private void observe(@Nonnull SCMHeadObserver observer, GitlabTag tag) {
        observe(observer, createTag(tag.getName(), tag.getCommit().getId(), settings.tagMonitorStrategy().buildUnmerged()));
    }

    private void observe(@Nonnull SCMHeadObserver observer, GitLabMergeRequest mergeRequest) {
        observe(observer,
                createMergeRequest(
                        String.valueOf(mergeRequest.getId()),
                        createBranch(mergeRequest.getSourceBranch(), mergeRequest.getSha()),
                        createBranch(mergeRequest.getTargetBranch())));
    }

    private void observe(@Nonnull SCMHeadObserver observer, GitLabSCMHead head) {
        observer.observe(head, head.getCommit());
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
