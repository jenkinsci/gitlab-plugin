package com.dabsquared.gitlabjenkins.trigger.branch;

import com.dabsquared.gitlabjenkins.GitLabProjectBranchesService;
import com.dabsquared.gitlabjenkins.Messages;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Job;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import jenkins.triggers.SCMTriggerItem;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Robin Müller
 */
public final class ProjectBranchesProvider {

    private static final Logger LOGGER = Logger.getLogger(ProjectBranchesProvider.class.getName());
    private static final ProjectBranchesProvider INSTANCE = new ProjectBranchesProvider();

    private ProjectBranchesProvider() {
    }

    public static ProjectBranchesProvider instance() {
        return INSTANCE;
    }

    public List<String> getProjectBranches(Job<?, ?> project) throws IOException {
        final URIish sourceRepository = getSourceRepoURLDefault(project);
        GitLabConnectionProperty connectionProperty = project.getProperty(GitLabConnectionProperty.class);
        if (connectionProperty != null && connectionProperty.getClient() != null) {
            return GitLabProjectBranchesService.instance().getBranches(connectionProperty.getClient(), sourceRepository.toString());
        } else {
            LOGGER.log(Level.WARNING, "getProjectBranches: gitlabHostUrl hasn't been configured globally. Job {0}.", project.getFullName());
            return Collections.emptyList();
        }
    }

    public AutoCompletionCandidates doAutoCompleteBranchesSpec(Job<?, ?> job, String query) {
        AutoCompletionCandidates result = new AutoCompletionCandidates();
        // show all suggestions for short strings
        if (query.length() < 2) {
            result.add(getProjectBranchesAsArray(job));
        } else {
            for (String branch : getProjectBranchesAsArray(job)) {
                if (branch.toLowerCase().contains(query.toLowerCase())) {
                    result.add(branch);
                }
            }
        }
        return result;
    }

    private String[] getProjectBranchesAsArray(Job<?, ?> job) {
        try {
            List<String> branches = getProjectBranches(job);
            return branches.toArray(new String[branches.size()]);
        } catch (final IllegalStateException ex) {
            LOGGER.log(Level.FINEST, "Unexpected IllegalStateException. Please check the logs and your configuration.", ex);
        } catch (final IOException ex) {
            LOGGER.log(Level.FINEST, "Unexpected IllegalStateException. Please check the logs and your configuration.", ex);
        }
        return new String[0];
    }


    /**
     * Get the URL of the first declared repository in the project configuration.
     * Use this as default source repository url.
     *
     * @return URIish the default value of the source repository url
     * @throws IllegalStateException Project does not use git scm.
     */
    private URIish getSourceRepoURLDefault(Job<?, ?> job) {
        SCMTriggerItem item = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(job);
        GitSCM gitSCM = getGitSCM(item);
        if (gitSCM == null) {
            LOGGER.log(Level.WARNING, "Could not find GitSCM for project. Project = {1}, next build = {2}",
                    array(job.getName(), String.valueOf(job.getNextBuildNumber())));
            throw new IllegalStateException("This project does not use git:" + job.getName());
        }
        return getFirstRepoURL(gitSCM.getRepositories());
    }

    private URIish getFirstRepoURL(List<RemoteConfig> repositories) {
        if (!repositories.isEmpty()) {
            List<URIish> uris = repositories.get(repositories.size() - 1).getURIs();
            if (!uris.isEmpty()) {
                return uris.get(uris.size() - 1);
            }
        }
        throw new IllegalStateException(Messages.GitLabPushTrigger_NoSourceRepository());
    }

    private GitSCM getGitSCM(SCMTriggerItem item) {
        if (item != null) {
            for (SCM scm : item.getSCMs()) {
                if (scm instanceof GitSCM) {
                    return (GitSCM) scm;
                }
            }
        }
        return null;
    }

    private Object[] array(Object... objects) {
        return objects;
    }
}
