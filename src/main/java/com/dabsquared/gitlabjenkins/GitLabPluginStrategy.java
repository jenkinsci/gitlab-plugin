/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabsquared.gitlabjenkins;

import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;

/**
 *
 * @author Pablo Bendersky
 */
public interface GitLabPluginStrategy {
    
    /**
     * Creates the Actions for the Push Request trigger.
     * @param pushTrigger The GitLabPushTrigger instance that is using this strategy.
     * @param req The Push Request, as received from GitLab
     * @param job The Jenkins Job
     * @return An Array of Actions to be used in the Jenkins Job.
     */
    Action[] createActions(GitLabPushTrigger pushTrigger, GitLabPushRequest req, Job job);

    /**
     * Creates the Action for the Merge Request trigger
     * @param pushTrigger The GitLabPushTrigger instance that is using this strategy.
     * @param req The Merge Request, as received from GitLab
     * @param job The Jenkins Job
     * @return A single action, to be used in Jenkins Job.
     */
    Action createAction(GitLabPushTrigger pushTrigger, GitLabMergeRequest req, Job job);
    
    /**
     * Runs after the web hook for push events has completed. Different strategies may either
     * ignore this method, or look up old builds to merge.
     * @param webHook Originating Web Hook
     * @param trigger Push Trigger instance
     * @param projectId The Project ID
     * @param projectRef The Project Ref
     */
    void buildOpenMergeRequestTriggeredByPush(GitLabWebHook webHook, GitLabPushTrigger trigger, Integer projectId, String projectRef);

    /**
     * Returns true if, in the current strategy, we need to skip a build for a Merge Request with a given state.
     * @param trigger Trigger instance, in case the strategy needs to inspect its settings.
     * @param state GitLab state of the Merge Request hook
     * @return true if we need to trigger a build for this state.
     */
    boolean shouldSkipMergeRequestBuild(GitLabPushTrigger trigger, String state);

    /**
     * Returns true if the found Merge Build has already been built. Depending on the strategy, this can be implemented in different ways.
     * @param request GitLabMergeRequest instance that triggered this build.
     * @param mergeBuild Found Merge Build for the commit.
     * @return true if the mergeBuild passed as parameter has already been built.
     */
    boolean isMergeBuildAlreadyBuilt(GitLabMergeRequest request, Run mergeBuild);

    /**
     * Returns true if this strategy skips existing builds through matching by SHA1.
     * @return true if this strategy skips existing builds through matching by SHA1, false otherwise.
     */
    boolean skipsExistingPushBuildsMatchingBySHA1();
    
}
