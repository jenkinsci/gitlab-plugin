package com.dabsquared.gitlabjenkins.trigger.branch;

import com.dabsquared.gitlabjenkins.Messages;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.service.GitLabProjectBranchesService;
import com.dabsquared.gitlabjenkins.trigger.WebHookRevisionParameterAction;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Item;
import hudson.model.Job;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    private List<String> getProjectBranches(Job<?, ?> project) {
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

    public FormValidation doCheckBranchesSpec(@AncestorInPath final Job<?, ?> project, @QueryParameter final String value) {
        if (!project.hasPermission(Item.CONFIGURE) || containsNoBranches(value)) {
            return FormValidation.ok();
        }

        try {
            return checkMatchingBranches(value, getProjectBranches(project));
        } catch (GitLabProjectBranchesService.BranchLoadingException e) {
            return FormValidation.warning(project.hasPermission(Jenkins.ADMINISTER) ? e : null, Messages.GitLabPushTrigger_CannotCheckBranches());
        }
    }

    private FormValidation checkMatchingBranches(@QueryParameter String value, List<String> projectBranches) {
        Set<String> matchingSpecs = new HashSet<>();
        Set<String> unknownSpecs = new HashSet<>();
        AntPathMatcherSet projectBranchesMatcherSet = new AntPathMatcherSet(projectBranches);
        for (String branchSpec : Splitter.on(',').omitEmptyStrings().trimResults().split(value)) {
            if (projectBranchesMatcherSet.contains(branchSpec)) {
                matchingSpecs.add(branchSpec);
            } else {
                unknownSpecs.add(branchSpec);
            }
        }

        if (unknownSpecs.isEmpty()) {
            return FormValidation.ok(Messages.GitLabPushTrigger_BranchesMatched(matchingSpecs.size()));
        } else {
            return FormValidation.warning(Messages.GitLabPushTrigger_BranchesNotFound(Joiner.on(", ").join(unknownSpecs)));
        }
    }

    private boolean containsNoBranches(@QueryParameter String value) {
        return StringUtils.isEmpty(value) || StringUtils.containsOnly(value, new char[]{',', ' '});
    }


    private String[] getProjectBranchesAsArray(Job<?, ?> job) {
        try {
            List<String> branches = getProjectBranches(job);
            return branches.toArray(new String[branches.size()]);
        } catch (GitLabProjectBranchesService.BranchLoadingException e) {
            LOGGER.log(Level.FINEST, "Failed to load branch names from GitLab. Please check the logs and your configuration.", e);
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
        List<WebHookRevisionParameterAction> actions = job.getActions(WebHookRevisionParameterAction.class);
        return getFirstRepoURL(actions);
    }

    private URIish getFirstRepoURL(List<WebHookRevisionParameterAction> actions) {
        if (!actions.isEmpty()) {
            WebHookRevisionParameterAction action = actions.get(actions.size() - 1);
            return action.getRepoURI();
        }
        throw new IllegalStateException(Messages.GitLabPushTrigger_NoSourceRepository());
    }

   
}
