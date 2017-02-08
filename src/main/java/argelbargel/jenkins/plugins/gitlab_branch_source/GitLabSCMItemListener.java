package argelbargel.jenkins.plugins.gitlab_branch_source;

import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.listeners.ItemListener;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import org.jenkinsci.plugins.workflow.multibranch.BranchJobProperty;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * This Listener adds/updates the {@link GitLabConnectionProperty} to the Jobs building the heads
 */
@SuppressWarnings("unused")
@Extension
public final class GitLabSCMItemListener extends ItemListener {
    private static final Logger LOGGER = Logger.getLogger(GitLabSCMItemListener.class.getName());

    @Override
    public void onCreated(Item item) {
        onUpdated(item);
    }

    @Override
    public void onUpdated(Item item) {
        if (item instanceof Job && item.getParent() instanceof SCMSourceOwner) {
            onUpdated((Job<?, ?>) item);
        }
    }

    private void onUpdated(Job<?, ?> job) {
        BranchJobProperty property = job.getProperty(BranchJobProperty.class);
        if (property != null) {
            SCMSource source = ((SCMSourceOwner) job.getParent()).getSCMSource(property.getBranch().getSourceId());
            if (source instanceof GitLabSCMSource) {
                updateGitLabConnectionProperty(job, ((GitLabSCMSource) source).getConnectionName());
            }
        }
    }

    private void updateGitLabConnectionProperty(Job<?, ?> job, String connectionName) {
        try {
            job.removeProperty(GitLabConnectionProperty.class);
            job.addProperty(new GitLabConnectionProperty(connectionName));
            job.save();
        } catch (IOException e) {
            LOGGER.warning("could not add gitlab-connection-property to job " + job.getName() + ": " + e.getMessage());
        }
    }
}
