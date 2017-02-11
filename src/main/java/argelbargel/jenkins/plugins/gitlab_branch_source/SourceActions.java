package argelbargel.jenkins.plugins.gitlab_branch_source;


import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPIException;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabMergeRequest;
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
import org.gitlab.api.models.GitlabProject;

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
    private final GitlabProject project;
    private final SourceSettings settings;


    SourceActions(GitlabProject project, SourceSettings settings) {
        this.project = project;
        this.settings = settings;
    }

    @Nonnull
    List<Action> retrieveSourceActions() throws IOException {
        return asList(
                new GitLabProjectMetadataAction(project),
                new GitLabProjectAvatarMetadataAction(project, settings.getConnectionName()),
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
            actions.add(GitLabLinkAction.toCommit(project, hash));
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
            GitLabMergeRequest mr = gitLabAPI(settings.getConnectionName()).getMergeRequest(project.getId(), ((GitLabSCMMergeRequestHead) head).getId());
            actions.add(new ObjectMetadataAction(mr.getTitle(), mr.getDescription(), mergeRequestUrl(project, ((GitLabSCMMergeRequestHead) head).getId())));
            actions.add(GitLabLinkAction.toMergeRequest(head.getPronoun(), project, mr.getId()));
            if (acceptMergeRequest(head)) {
                boolean removeSourceBranch = mr.getRemoveSourceBranch() || removeSourceBranch(head);
                actions.add(new GitLabSCMAcceptMergeRequestAction(mr.getProjectId(), mr.getId(), settings.getMergeCommitMessage(), removeSourceBranch));
            }

        } else {
            actions.add(new ObjectMetadataAction(head.getName(), "", treeUrl(project, head.getName())));
            actions.add(GitLabLinkAction.toTree(head.getPronoun(), project, head.getName()));
        }

        actions.add(new GitLabSCMPublishAction(
                settings.getUpdateBuildDescription(),
                buildStatusPublishMode(head),
                settings.getPublishUnstableBuildsAsSuccess(),
                settings.getPublisherName()
        ));

        if (head instanceof GitLabSCMBranchHead && StringUtils.equals(project.getDefaultBranch(), head.getName())) {
            actions.add(new PrimaryInstanceMetadataAction());
        }

        return actions;
    }

    private BuildStatusPublishMode buildStatusPublishMode(SCMHead head) {
        if (head instanceof GitLabSCMMergeRequestHead) {
            return ((GitLabSCMMergeRequestHead) head).fromOrigin()
                    ? settings.originMonitorStrategy().getBuildStatusPublishMode()
                    : settings.forksMonitorStrategy().getBuildStatusPublishMode();
        } else if (head instanceof TagSCMHead) {
            return settings.tagMonitorStrategy().getBuildStatusPublishMode();
        }

        return settings.branchMonitorStrategy().getBuildStatusPublishMode();
    }

    private boolean acceptMergeRequest(SCMHead head) {
        if (head instanceof GitLabSCMMergeRequestHead) {
            GitLabSCMMergeRequestHead mergeRequest = (GitLabSCMMergeRequestHead) head;
            return mergeRequest.isMerged() &&
                    settings.determineMergeRequestStrategyValue(
                            mergeRequest,
                            settings.originMonitorStrategy().getAcceptMergeRequests(),
                            settings.forksMonitorStrategy().getAcceptMergeRequests());
        }
        return false;
    }

    private boolean removeSourceBranch(SCMHead head) {
        if (head instanceof GitLabSCMMergeRequestHead) {
            GitLabSCMMergeRequestHead mergeRequest = (GitLabSCMMergeRequestHead) head;
            return mergeRequest.isMerged() && mergeRequest.fromOrigin() && settings.originMonitorStrategy().getRemoveSourceBranch();
        }

        return false;
    }

}
