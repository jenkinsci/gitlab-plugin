package argelbargel.jenkins.plugins.gitlab_branch_source;

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
import org.apache.commons.lang.StringUtils;
import org.gitlab.api.models.GitlabProject;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabAPI;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabLinkAction.mergeRequestUrl;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabLinkAction.treeUrl;


class SourceActions {
    private final GitlabProject project;
    private final SourceSettings settings;

    SourceActions(GitlabProject project, SourceSettings settings) {
        this.project = project;
        this.settings = settings;
    }

    @Nonnull
    List<Action> retrieveSourceActions() throws IOException {
        return Arrays.asList(
                new GitLabProjectMetadataAction(project),
                new GitLabProjectAvatarMetadataAction(project, settings.getConnectionName()),
                GitLabLinkAction.toProject(Messages.GitLabSCMSource_Pronoun(), project));

    }

    @Nonnull
    List<Action> retrieveHeadActions(@Nonnull SCMHead head, @CheckForNull SCMHeadEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        if (head instanceof HeadBuildMode) {
            return retrieveHeadActions(((HeadBuildMode) head).getHead(), event, listener);
        }

        List<Action> actions = new ArrayList<>();
        if (event instanceof GitLabSCMEvent) {
            actions.add(new GitLabSCMCauseAction(((GitLabSCMEvent) event).getCause(), settings.getUpdateBuildDescription()));
        }

        if (head instanceof GitLabSCMMergeRequestHead) {
            listener.getLogger().format(Messages.GitLabSCMSource_retrievingMergeRequest(((GitLabSCMMergeRequestHead) head).getId()));
            GitLabMergeRequest mr = gitLabAPI(settings.getConnectionName()).getMergeRequest(project.getId(), ((GitLabSCMMergeRequestHead) head).getId());
            actions.add(new ObjectMetadataAction(mr.getTitle(), mr.getDescription(), mergeRequestUrl(project, ((GitLabSCMMergeRequestHead) head).getId())));
            actions.add(GitLabLinkAction.toMergeRequest(head.getPronoun(), project, mr.getId()));
        } else {
            actions.add(new ObjectMetadataAction(head.getName(), "", treeUrl(project, head.getName())));
            actions.add(GitLabLinkAction.toTree(head.getPronoun(), project, head.getName()));
        }

        if (head instanceof GitLabSCMBranchHead && StringUtils.equals(project.getDefaultBranch(), head.getName())) {
            actions.add(new PrimaryInstanceMetadataAction());
        }

        return actions;
    }

    @Nonnull
    List<Action> retrieve(@Nonnull SCMRevision revision, @CheckForNull SCMHeadEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        if (revision.getHead() instanceof HeadBuildMode) {
            return retrieveHeadActions(((HeadBuildMode) revision.getHead()).getHead(), event, listener);
        }

        List<Action> actions = new ArrayList<>();
        if (event instanceof GitLabSCMEvent) {
            actions.add(new GitLabSCMCauseAction(((GitLabSCMEvent) event).getCause(), settings.getUpdateBuildDescription()));
        }


        if (revision instanceof SCMRevisionImpl) {
            String hash = ((SCMRevisionImpl) revision).getHash();
            actions.add(GitLabLinkAction.toCommit(project, hash));
        }

        return actions;
    }
}
