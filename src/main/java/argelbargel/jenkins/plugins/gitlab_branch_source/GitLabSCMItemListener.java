package argelbargel.jenkins.plugins.gitlab_branch_source;


import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import hudson.BulkChange;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Saveable;
import hudson.model.listeners.ItemListener;
import jenkins.branch.BranchBuildStrategy;
import jenkins.branch.BranchSource;
import jenkins.branch.MultiBranchProject;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This Listener updates the Jobs using GitLabSCMSources
 */
@Extension
public final class GitLabSCMItemListener extends ItemListener {
    private static final Logger LOGGER = Logger.getLogger(GitLabSCMItemListener.class.getName());

    @Override
    public void onCreated(Item item) {
        ConditionalBulkChange changes = new ConditionalBulkChange(item); // TODO: check if change-detection is built-in
        try {
            // TODO: HACK ALERT! There must/should be a nicer way to do this!
            if (item instanceof MultiBranchProject) {
                changes.mustSave(updateBranchBuildStrategies((MultiBranchProject<?, ?>) item));
            }
            onUpdated(item, changes); // also add the gitlab-connection-property
        } finally {
            changes.save();
        }
    }

    @Override
    public void onUpdated(Item item) {
        ConditionalBulkChange changes = new ConditionalBulkChange(item); // TODO: check if change-detection is built-in
        try {
            onUpdated(item, changes);
        } finally {
            changes.save();
        }
    }

    private boolean updateBranchBuildStrategies(MultiBranchProject<?, ?> project) {
        boolean changed = false;

        for (BranchSource branchSource : project.getSources()) {
            if (GitLabSCMBranchBuildStrategy.INSTANCE.isApplicable(branchSource)) {
                List<BranchBuildStrategy> strategies = new ArrayList<>(branchSource.getBuildStrategies());
                if (!strategies.contains(GitLabSCMBranchBuildStrategy.INSTANCE)) {
                    strategies.add(GitLabSCMBranchBuildStrategy.INSTANCE);
                    branchSource.setBuildStrategies(strategies);
                    changed = true;
                }
            }
        }

        return changed;
    }

    private void onUpdated(Item item, ConditionalBulkChange changes) {
        if (item instanceof Job) {
            onUpdated((Job<?, ?>) item, changes);
        }
    }

    private void onUpdated(Job<?, ?> job, ConditionalBulkChange changes) {
        BranchJobProperty property = job.getProperty(BranchJobProperty.class);
        if (property != null && job.getParent() instanceof SCMSourceOwner) {
            changes.mustSave(updateGitLabConnectionProperty(job, (SCMSourceOwner) job.getParent(), property.getBranch().getSourceId()));
        }
    }

    private boolean updateGitLabConnectionProperty(Job<?, ?> job, SCMSourceOwner sourceOwner, String sourceId) {
        SCMSource source = ((SCMSourceOwner) job.getParent()).getSCMSource(sourceId);
        if (source instanceof GitLabSCMSource) {
            String connectionName = ((GitLabSCMSource) source).getConnectionName();
            GitLabConnectionProperty property = job.getProperty(GitLabConnectionProperty.class);
            if (property == null || !connectionName.equals(property.getGitLabConnection())) {
                updateGitLabConnectionProperty(job, connectionName);
                return true;
            }
        }

        return false;
    }

    private void updateGitLabConnectionProperty(Job<?, ?> job, String connectionName) {
        try {
            job.removeProperty(GitLabConnectionProperty.class);
            job.addProperty(new GitLabConnectionProperty(connectionName));
        } catch (IOException e) {
            LOGGER.warning("could not update gitlab-connection-property to job " + job.getName() + ": " + e.getMessage());
        }
    }


    private static final class ConditionalBulkChange extends BulkChange {
        private boolean changed = false;

        ConditionalBulkChange(Saveable saveable) {
            super(saveable);
            changed = false;
        }

        void mustSave(boolean value) {
            changed |= value;
        }

        public void save() {
            if (!changed) {
                abort();
            } else {
                try {
                    super.commit();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "could not save changes", e);
                }
            }
        }
    }
}
