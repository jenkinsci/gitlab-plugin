package argelbargel.jenkins.plugins.gitlab_branch_source;


import argelbargel.jenkins.plugins.gitlab_branch_source.actions.GitLabLinkAction;
import argelbargel.jenkins.plugins.gitlab_branch_source.actions.GitLabProjectAvatarMetadataAction;
import argelbargel.jenkins.plugins.gitlab_branch_source.actions.GitLabSCMAcceptMergeRequestAction;
import argelbargel.jenkins.plugins.gitlab_branch_source.actions.GitLabSCMCauseAction;
import argelbargel.jenkins.plugins.gitlab_branch_source.actions.GitLabSCMHeadMetadataAction;
import argelbargel.jenkins.plugins.gitlab_branch_source.actions.GitLabSCMPublishAction;
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
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;


class SourceActions {
    private final GitLabSCMSource source;

    SourceActions(GitLabSCMSource source) {
        this.source = source;
    }

    @Nonnull
    List<Action> retrieve(@CheckForNull SCMSourceEvent event, @Nonnull TaskListener listener) throws IOException {
        GitLabProject project = source.getProject();
        return asList(
                new ObjectMetadataAction(project.getNameWithNamespace(), project.getDescription(), project.getWebUrl()),
                new GitLabProjectAvatarMetadataAction(project.getId(), source.getConnectionName()),
                GitLabLinkAction.toProject(project));
    }

    @Nonnull
    List<Action> retrieve(@Nonnull SCMHead head, @CheckForNull SCMHeadEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        if (head instanceof GitLabSCMHead) {
            return retrieve((GitLabSCMHead) head, event, listener);
        }

        return emptyList();
    }

    @Nonnull
    private List<Action> retrieve(@Nonnull GitLabSCMHead head, @CheckForNull SCMHeadEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        List<Action> actions = new ArrayList<>();

        actions.add(new GitLabSCMPublishAction(
                source.getUpdateBuildDescription(),
                buildStatusPublishMode(head),
                source.getPublishUnstableBuildsAsSuccess(),
                source.getPublisherName()
        ));

        Action linkAction;

        if (head instanceof ChangeRequestSCMHead) {
            linkAction = GitLabLinkAction.toMergeRequest(source.getProject(), ((ChangeRequestSCMHead) head).getId());

            GitLabMergeRequest mr = retrieveMergeRequest((ChangeRequestSCMHead) head, listener);
            if (acceptMergeRequest(head)) {
                boolean removeSourceBranch = mr.getRemoveSourceBranch() || removeSourceBranch(head);
                actions.add(new GitLabSCMAcceptMergeRequestAction(mr.getProjectId(), mr.getId(), source.getMergeCommitMessage(), removeSourceBranch));
            }
        } else {
            linkAction = (head instanceof TagSCMHead) ? GitLabLinkAction.toTag(source.getProject(), head.getName()) : GitLabLinkAction.toBranch(source.getProject(), head.getName());
            if (head instanceof GitLabSCMBranchHead && StringUtils.equals(source.getProject().getDefaultBranch(), head.getName())) {
                actions.add(new PrimaryInstanceMetadataAction());
            }
        }

        actions.add(new GitLabSCMHeadMetadataAction(head, linkAction.getUrlName()));
        actions.add(linkAction);
        return actions;
    }


    @Nonnull
    List<Action> retrieve(@Nonnull SCMRevision revision, @CheckForNull SCMHeadEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        if (revision instanceof SCMRevisionImpl) {
            return retrieve((SCMRevisionImpl) revision, event, listener);
        }

        return emptyList();
    }

    @Nonnull
    private List<Action> retrieve(@Nonnull SCMRevisionImpl revision, @CheckForNull SCMHeadEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        List<Action> actions = new ArrayList<>();

        String hash = revision.getHash();
        Action linkAction = GitLabLinkAction.toCommit(source.getProject(), hash);
        actions.add(new GitLabSCMHeadMetadataAction((GitLabSCMHead) revision.getHead(), linkAction.getUrlName(), hash));
        actions.add(linkAction);

        if (event instanceof GitLabSCMEvent) {
            actions.add(new GitLabSCMCauseAction(((GitLabSCMEvent) event).getCause()));
        }

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
