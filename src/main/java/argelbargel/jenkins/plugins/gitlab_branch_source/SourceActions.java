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
import jenkins.scm.api.metadata.ObjectMetadataAction;
import jenkins.scm.api.metadata.PrimaryInstanceMetadataAction;
import jenkins.scm.api.mixin.TagSCMHead;
import org.apache.commons.lang.StringUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabAPI;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabLinkAction.mergeRequestUrl;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabLinkAction.treeUrl;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead.REVISION_HEAD;
import static java.util.Arrays.asList;


class SourceActions {
    private final GitLabSCMSource source;

    SourceActions(GitLabSCMSource source) {
        this.source = source;
    }

    @Nonnull
    List<Action> retrieveSourceActions() throws IOException {
        GitLabProject project = source.getProject();
        return asList(
                new ObjectMetadataAction(null, project.getDescription(), project.getWebUrl()),
                new GitLabProjectAvatarMetadataAction(project.getId(), source.getConnectionName()),
                GitLabLinkAction.toProject(Messages.GitLabSCMSource_Pronoun(), project));
    }

    @Nonnull
    List<Action> retrieve(@Nonnull SCMHead head, @CheckForNull SCMHeadEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        return retrieve(new SCMRevisionImpl(head, REVISION_HEAD), event, listener);
    }

    @Nonnull
    List<Action> retrieve(@Nonnull SCMRevision revision, @CheckForNull SCMHeadEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        List<Action> actions = new ArrayList<>();

        actions.addAll(retrieveHeadActions(revision.getHead(), listener));

        if (revision instanceof SCMRevisionImpl) {
            String hash = ((SCMRevisionImpl) revision).getHash();
            actions.add(GitLabLinkAction.toCommit(source.getProject(), hash));
            actions.add(
                    (event instanceof GitLabSCMEvent)
                            ? new GitLabSCMCauseAction((SCMRevisionImpl) revision, ((GitLabSCMEvent) event).getCause())
                            : new GitLabSCMCauseAction((SCMRevisionImpl) revision));
        }

        return actions;
    }

    private List<Action> retrieveHeadActions(@Nonnull SCMHead head, @Nonnull TaskListener listener) throws GitLabAPIException {
        List<Action> actions = new ArrayList<>();

        if (head instanceof GitLabSCMMergeRequestHead) {
            listener.getLogger().format(Messages.GitLabSCMSource_retrievingMergeRequest(((GitLabSCMMergeRequestHead) head).getId()) + "\n");
            GitLabMergeRequest mr = gitLabAPI(source.getConnectionName()).getMergeRequest(source.getProjectId(), ((GitLabSCMMergeRequestHead) head).getId());
            actions.add(new ObjectMetadataAction(mr.getTitle(), mr.getDescription(), mergeRequestUrl(source.getProject(), ((GitLabSCMMergeRequestHead) head).getId())));
            actions.add(GitLabLinkAction.toMergeRequest(head.getPronoun(), source.getProject(), mr.getId()));
            if (acceptMergeRequest(head)) {
                boolean removeSourceBranch = mr.getRemoveSourceBranch() || removeSourceBranch(head);
                actions.add(new GitLabSCMAcceptMergeRequestAction(mr.getProjectId(), mr.getId(), source.getMergeCommitMessage(), removeSourceBranch));
            }

        } else {
            actions.add(new ObjectMetadataAction(head.getName(), "", treeUrl(source.getProject(), head.getName())));
            actions.add(GitLabLinkAction.toTree(head.getPronoun(), source.getProject(), head.getName()));
        }

        actions.add(new GitLabSCMPublishAction(
                source.getUpdateBuildDescription(),
                buildStatusPublishMode(head),
                source.getPublishUnstableBuildsAsSuccess(),
                source.getPublisherName()
        ));

        if (head instanceof GitLabSCMBranchHead && StringUtils.equals(source.getProject().getDefaultBranch(), head.getName())) {
            actions.add(new PrimaryInstanceMetadataAction());
        }

        return actions;
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
