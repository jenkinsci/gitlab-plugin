package argelbargel.jenkins.plugins.gitlab_branch_source;


import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPIException;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabMergeRequest;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabProject;
import argelbargel.jenkins.plugins.gitlab_branch_source.events.GitLabSCMEvent;
import hudson.model.Action;
import hudson.model.TaskListener;
import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSourceEvent;
import jenkins.scm.api.metadata.ObjectMetadataAction;
import jenkins.scm.api.metadata.PrimaryInstanceMetadataAction;
import jenkins.scm.api.mixin.ChangeRequestSCMHead;
import jenkins.scm.api.mixin.TagSCMHead;
import org.apache.commons.lang.StringUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabAPI;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead.REVISION_HEAD;
import static java.util.Arrays.asList;


class SourceActions {
    private final GitLabSCMSource source;

    SourceActions(GitLabSCMSource source) {
        this.source = source;
    }

    @Nonnull
    List<Action> retrieveActions(@CheckForNull SCMSourceEvent event, @Nonnull TaskListener listener) throws IOException {
        GitLabProject project = source.getProject();
        return asList(
                new ObjectMetadataAction(null, project.getDescription(), project.getWebUrl()),
                new GitLabProjectAvatarMetadataAction(project.getId(), source.getConnectionName()),
                GitLabLinkAction.toProject(project));
    }

    @Nonnull
    List<Action> retrieve(@Nonnull SCMHead head, @CheckForNull SCMHeadEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        List<Action> actions = new ArrayList<>();

        if (head instanceof ChangeRequestSCMHead) {
            GitLabMergeRequest mr = retrieveMergeRequest((ChangeRequestSCMHead) head, listener);
            Action linkAction = GitLabLinkAction.toMergeRequest(source.getProject(), ((ChangeRequestSCMHead) head).getId());
            actions.add(new ObjectMetadataAction(mr.getTitle(), mr.getDescription(), linkAction.getUrlName()));
            actions.add(linkAction);
        } else {
            Action linkAction = (head instanceof TagSCMHead) ? GitLabLinkAction.toTag(source.getProject(), head.getName()) : GitLabLinkAction.toBranch(source.getProject(), head.getName());
            actions.add(new ObjectMetadataAction(head.getName(), "", linkAction.getUrlName()));
            actions.add(linkAction);
            if (head instanceof GitLabSCMBranchHead && StringUtils.equals(source.getProject().getDefaultBranch(), head.getName())) {
                actions.add(new PrimaryInstanceMetadataAction());
            }
        }

        SCMRevisionImpl rev = (head instanceof GitLabSCMHead) ? ((GitLabSCMHead) head).getRevision() : new SCMRevisionImpl(head, REVISION_HEAD);
        actions.addAll(retrieveRevisionActions(rev, event, listener)); // TODO: why is this neccessary? Would be nicer if those actions were added to the run

        return actions;
    }

    @Nonnull
    List<Action> retrieve(@Nonnull SCMRevision revision, @CheckForNull SCMHeadEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        List<Action> actions = new ArrayList<>();

        if (revision instanceof SCMRevisionImpl) {
            String hash = ((SCMRevisionImpl) revision).getHash();
            Action linkAction = GitLabLinkAction.toCommit(source.getProject(), hash);
            actions.add(new ObjectMetadataAction(hash, "", linkAction.getUrlName()));
            actions.add(linkAction);
            actions.addAll(retrieveRevisionActions((SCMRevisionImpl) revision, event, listener));
        }

        return actions;
    }

    @Nonnull
    private List<Action> retrieveRevisionActions(@Nonnull SCMRevisionImpl revision, @CheckForNull SCMHeadEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        List<Action> actions = new ArrayList<>();

        actions.add(new GitLabSCMPublishAction(
                source.getUpdateBuildDescription(),
                buildStatusPublishMode(revision.getHead()),
                source.getPublishUnstableBuildsAsSuccess(),
                source.getPublisherName()
        ));

        if (revision.getHead() instanceof ChangeRequestSCMHead) {
            if (acceptMergeRequest(revision.getHead())) {
                GitLabMergeRequest mr = retrieveMergeRequest((ChangeRequestSCMHead) revision.getHead(), listener);
                boolean removeSourceBranch = mr.getRemoveSourceBranch() || removeSourceBranch(revision.getHead());
                actions.add(new GitLabSCMAcceptMergeRequestAction(mr.getProjectId(), mr.getId(), source.getMergeCommitMessage(), removeSourceBranch));
            }
        }

        actions.add(
                (event instanceof GitLabSCMEvent)
                        ? new GitLabSCMCauseAction(revision, ((GitLabSCMEvent) event).getCause())
                        : new GitLabSCMCauseAction(revision));

        return actions;
    }

    private GitLabMergeRequest retrieveMergeRequest(@Nonnull ChangeRequestSCMHead head, @Nonnull TaskListener listener) throws GitLabAPIException {
        listener.getLogger().format(Messages.GitLabSCMSource_retrievingMergeRequest(head.getId()) + "\n");
        return gitLabAPI(source.getConnectionName()).getMergeRequest(source.getProjectId(), head.getId());
    }

    private BuildStatusPublishMode buildStatusPublishMode(SCMHead head) {
        if (head instanceof GitLabSCMMergeRequestHead) {
            return ((GitLabSCMMergeRequestHead) head).fromOrigin()
                    ? source.getOriginBuildStatusPublishMode()
                    : source.getForkBuildStatusPublishMode();
        } else if (head instanceof TagSCMHead) {
            return source.getTagBuildStatusPublishMode();
        }

        return source.getBranchBuildStatusPublishMode();
    }

    private boolean acceptMergeRequest(SCMHead head) {
        if (head instanceof GitLabSCMMergeRequestHead) {
            GitLabSCMMergeRequestHead mergeRequest = (GitLabSCMMergeRequestHead) head;
            return mergeRequest.isMerged() &&
                    source.determineMergeRequestStrategyValue(
                            mergeRequest,
                            source.getAcceptMergeRequestsFromOrigin(),
                            source.getAcceptMergeRequestsFromForks());
        }
        return false;
    }

    private boolean removeSourceBranch(SCMHead head) {
        if (head instanceof GitLabSCMMergeRequestHead) {
            GitLabSCMMergeRequestHead mergeRequest = (GitLabSCMMergeRequestHead) head;
            return mergeRequest.isMerged() && mergeRequest.fromOrigin() && source.getRemoveSourceBranchFromOrigin();
        }

        return false;
    }
}
