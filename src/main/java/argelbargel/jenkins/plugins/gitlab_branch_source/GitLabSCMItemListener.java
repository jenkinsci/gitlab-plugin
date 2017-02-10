package argelbargel.jenkins.plugins.gitlab_branch_source;


import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
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
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;


/**
 * This Listener updates the Jobs using GitLabSCMSources
 */
@Extension
public final class GitLabSCMItemListener extends ItemListener {
    private static final Logger LOGGER = Logger.getLogger(GitLabSCMItemListener.class.getName());

    @Override
    public void onCreated(Item item) {
            // TODO: HACK ALERT! There must/should be a nicer way to do this!
            if (item instanceof MultiBranchProject) {
                if (updateBranchBuildStrategies((MultiBranchProject<?, ?>) item)) {
                    try {
                        item.save();
                    } catch (IOException e) {
                        LOGGER.log(SEVERE, "error saving changes to " + item, e);
                    }
                }
            }

        onUpdated(item); // also add the properties
    }

    @Override
    public void onUpdated(Item item) {
        if (item instanceof Job) {
            onUpdated((Job<?, ?>) item);
        }
    }

    private void onUpdated(Job<?, ?> job) {
        BranchJobProperty property = job.getProperty(BranchJobProperty.class);
        if (property != null && job.getParent() instanceof SCMSourceOwner) {
            // TODO: HACK ALERT! There must/should be a nicer way to do this!
            updateProperties(job, (SCMSourceOwner) job.getParent(), property.getBranch().getSourceId());
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


    private boolean updateProperties(Job<?, ?> job, SCMSourceOwner sourceOwner, String sourceId) {
        SCMSource source = sourceOwner.getSCMSource(sourceId);
        if (source instanceof GitLabSCMSource) {
            String connectionName = ((GitLabSCMSource) source).getConnectionName();
            GitLabConnectionProperty property = job.getProperty(GitLabConnectionProperty.class);
            if (property == null || !connectionName.equals(property.getGitLabConnection())) {
                updateProperties(job, connectionName);
                return true;
            }
        }

        return false;
    }

    private void updateProperties(Job<?, ?> job, String connectionName) {
        try {
            job.removeProperty(GitLabConnectionProperty.class);
            job.addProperty(new GitLabConnectionProperty(connectionName));
        } catch (IOException e) {
            LOGGER.warning("could not update gitlab-connection-property to job " + job.getName() + ": " + e.getMessage());
        }
    }
}
