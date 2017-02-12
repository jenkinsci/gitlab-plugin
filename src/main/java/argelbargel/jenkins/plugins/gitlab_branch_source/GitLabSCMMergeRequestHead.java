package argelbargel.jenkins.plugins.gitlab_branch_source;


import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPIException;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitException;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.Revision;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.util.MergeRecord;
import jenkins.scm.api.mixin.ChangeRequestSCMHead;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.gitclient.CheckoutCommand;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.jenkinsci.plugins.gitclient.MergeCommand;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMRefSpec.MERGE_REQUESTS;


public final class GitLabSCMMergeRequestHead extends GitLabSCMHeadImpl implements ChangeRequestSCMHead {
    static final String CAN_BE_MERGED = "can_be_merged";

    private final int id;
    private final String title;
    private final GitLabSCMHead sourceBranch;
    private final GitLabSCMBranchHead targetBranch;
    private final boolean mergeable;
    private final boolean merge;


    GitLabSCMMergeRequestHead(int id, String title, GitLabSCMHead source, GitLabSCMBranchHead target, boolean mergeable) {
        this(id, title, source, target, mergeable, false);
    }

    private GitLabSCMMergeRequestHead(int id, String title, GitLabSCMHead source, GitLabSCMBranchHead target, boolean mergeable, boolean merge) {
        super(source.getProjectId(), title + " (!" + id + ")" + (merge ? " merged" : ""), source.getRevision().getHash(), Messages.GitLabSCMMergeRequest_Pronoun(), MERGE_REQUESTS);
        this.id = id;
        this.title = title;
        this.sourceBranch = source;
        this.targetBranch = target;
        this.mergeable = mergeable;
        this.merge = merge;
    }

    @Nonnull
    @Override
    public String getId() {
        return String.valueOf(id);
    }

    @Nonnull
    @Override
    public GitLabSCMHead getTarget() {
        return targetBranch;
    }

    public GitLabSCMMergeRequestHead merged() {
        return new GitLabSCMMergeRequestHead(id, title, sourceBranch, targetBranch, mergeable, true);
    }

    GitLabSCMHead getSource() {
        return sourceBranch;
    }

    @Nonnull
    @Override
    String getRef() {
        return getRefSpec().destinationRef(sourceBranch.getName());
    }


    public boolean fromOrigin() {
        return getProjectId() == getTarget().getProjectId();
    }

    boolean isMergeable() {
        return mergeable;
    }

    boolean isMerged() {
        return merge;
    }

    @Nonnull
    @Override
    List<UserRemoteConfig> getRemotes(@Nonnull GitLabSCMSource source) throws GitLabAPIException {
        List<UserRemoteConfig> remotes = new ArrayList<>(2);
        remotes.add(new UserRemoteConfig(
                getProject(getProjectId(), source).getRemote(source),
                "merge-request", "",
                source.getCredentialsId()));
        if (merge) {
            remotes.addAll(targetBranch.getRemotes(source));
        }

        return remotes;
    }

    @Nonnull
    @Override
    List<BranchSpec> getBranchSpecs() {
        if (!merge) {
            return super.getBranchSpecs();
        }

        List<BranchSpec> branches = new ArrayList<>(2);
        branches.addAll(super.getBranchSpecs());
        branches.add(new BranchSpec(targetBranch.getRef()));
        return branches;
    }

    @Nonnull
    @Override
    List<GitSCMExtension> getExtensions(GitLabSCMSource source) {
        return merge ? Collections.<GitSCMExtension>singletonList(new MergeWith()) : Collections.<GitSCMExtension>emptyList();
    }


    private class MergeWith extends GitSCMExtension {
        @Override
        public Revision decorateRevisionToBuild(GitSCM scm, Run<?, ?> build, GitClient git, TaskListener listener, Revision marked, Revision rev) throws IOException, InterruptedException, GitException {
            listener.getLogger().println("Merging " + targetBranch.getName() + " commit " + targetBranch.getRevision().getHash() + " into merge-request head commit " + rev.getSha1String());
            checkout(scm, build, git, listener, rev);
            try {
                git.setAuthor("Jenkins", /* could parse out of JenkinsLocationConfiguration.get().getAdminAddress() but seems overkill */"nobody@nowhere");
                git.setCommitter("Jenkins", "nobody@nowhere");
                MergeCommand cmd = git.merge().setRevisionToMerge(ObjectId.fromString(targetBranch.getRevision().getHash()));
                for (GitSCMExtension ext : scm.getExtensions()) {
                    // By default we do a regular merge, allowing it to fast-forward.
                    ext.decorateMergeCommand(scm, build, git, listener, cmd);
                }
                cmd.execute();
            } catch (GitException e) {
                // Try to revert merge conflict markers.
                checkout(scm, build, git, listener, rev);
                throw e;
            }
            build.addAction(new MergeRecord(targetBranch.getRefSpec().destinationRef(targetBranch.getName()), targetBranch.getRevision().getHash())); // does not seem to be used, but just in case
            ObjectId mergeRev = git.revParse(Constants.HEAD);
            listener.getLogger().println("Merge succeeded, producing " + mergeRev.name());
            return new Revision(mergeRev, rev.getBranches()); // note that this ensures Build.revision != Build.marked
        }

        private void checkout(GitSCM scm, Run<?, ?> build, GitClient git, TaskListener listener, Revision rev) throws InterruptedException, IOException, GitException {
            CheckoutCommand checkoutCommand = git.checkout().ref(rev.getSha1String());
            for (GitSCMExtension ext : scm.getExtensions()) {
                ext.decorateCheckoutCommand(scm, build, git, listener, checkoutCommand);
            }
            checkoutCommand.execute();
        }
    }
}
