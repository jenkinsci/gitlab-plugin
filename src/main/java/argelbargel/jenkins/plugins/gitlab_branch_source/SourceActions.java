package argelbargel.jenkins.plugins.gitlab_branch_source;

import argelbargel.jenkins.plugins.gitlab_branch_source.events.GitLabSCMEvent;
import hudson.model.Action;
import hudson.model.TaskListener;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSourceEvent;
import jenkins.scm.api.metadata.ObjectMetadataAction;
import org.gitlab.api.models.GitlabProject;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabLink.mergeRequestUrl;
import static jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;


class SourceActions {
    private final GitlabProject project;
    private final SourceSettings settings;

    SourceActions(GitlabProject project, SourceSettings settings) {
        this.project = project;
        this.settings = settings;
    }

    @Nonnull
    List<Action> retrieve(@CheckForNull SCMSourceEvent event, @Nonnull TaskListener listener) throws IOException {
        return Arrays.asList(
                new ObjectMetadataAction(null, project.getDescription(), project.getWebUrl()),
                new ObjectMetadataAction(Messages.GitLabSCMSource_DefaultBranch(), project.getDefaultBranch(), GitLabLink.treeUrl(project, project.getDefaultBranch())),
                GitLabLink.toProject(project),
                GitLabLink.toTree(project, project.getDefaultBranch()));

    }

    @Nonnull
    List<Action> retrieve(@Nonnull SCMHead head, @CheckForNull SCMHeadEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        if (head instanceof HeadLabel) {
            return retrieve(((HeadLabel) head).getHead(), event, listener);
        }

        List<Action> actions = new ArrayList<>();
        actions.add(GitLabLink.toTree(project, head.getName()));

        if (head instanceof GitLabSCMMergeRequestHead) {
            actions.add(new ObjectMetadataAction(head.getName(), "testeststeststeststes", mergeRequestUrl(project, ((GitLabSCMMergeRequestHead) head).getId())));
            actions.add(GitLabLink.toMergeRequest(project, ((GitLabSCMMergeRequestHead) head).getId()));
        }

        if (event instanceof GitLabSCMEvent) {
            actions.add(new GitLabSCMCauseAction(((GitLabSCMEvent) event).getCause(), settings.getUpdateBuildDescription()));
        }


        return actions;
    }

    @Nonnull
    List<Action> retrieve(@Nonnull SCMRevision revision, @CheckForNull SCMHeadEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        if (revision.getHead() instanceof HeadLabel) {
            return retrieve(((HeadLabel) revision.getHead()).getHead(), event, listener);
        }

        List<Action> actions = new ArrayList<>();
        if (event instanceof GitLabSCMEvent) {
            actions.add(new GitLabSCMCauseAction(((GitLabSCMEvent) event).getCause(), settings.getUpdateBuildDescription()));
        }

        if (revision instanceof SCMRevisionImpl) {
            String hash = ((SCMRevisionImpl) revision).getHash();
            actions.add(GitLabLink.toCommit(project, hash));
            actions.add(GitLabLink.toTree(project, hash));
        } else {
            actions.add(GitLabLink.toTree(project, revision.getHead().getName()));
        }

        return actions;
    }

}
