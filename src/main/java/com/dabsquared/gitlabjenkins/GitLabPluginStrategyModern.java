/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabsquared.gitlabjenkins;

import static com.dabsquared.gitlabjenkins.GitLabPushTrigger.getDesc;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.plugins.git.RevisionParameterAction;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.ArrayUtils;

/**
 *
 * @author Pablo Bendersky
 */
public class GitLabPluginStrategyModern implements GitLabPluginStrategy {
    private static final Logger LOGGER = Logger.getLogger(GitLabPluginStrategyModern.class.getName());

    @Override
    public Action[] createActions(GitLabPushTrigger pushTrigger, GitLabPushRequest req, Job job) {
        ArrayList<Action> actions = new ArrayList<Action>();
	    actions.add(new CauseAction(pushTrigger.createGitLabPushCause(req)));

        String branch = pushTrigger.getSourceBranch(req);

        LOGGER.log(Level.INFO, "GitLab Push Request from branch {0}.", branch);
        
        RevisionParameterAction revision;
        revision = pushTrigger.createPushRequestRevisionParameter(job, req);
        if (revision == null) {
            return null;
        }

        actions.add(revision);
        Action[] actionsArray = actions.toArray(new Action[0]);

        return actionsArray;    
    }

    @Override
    public Action createAction(GitLabPushTrigger pushTrigger, GitLabMergeRequest req, Job job) {
        RevisionParameterAction action;
        action = new RevisionParameterAction(String.format("refs/remotes/origin/merge-requests/%s", req.getObjectAttribute().getIid()), getDesc().getSourceRepoURLDefault(job));
        
        return action;
    }

    @Override
    public void buildOpenMergeRequestTriggeredByPush(GitLabWebHook webHook, GitLabPushTrigger trigger, Integer projectId, String projectRef) {
        // Intentionally blank. In this strategy, merge requests are not handled _at all_ when the build was triggered by a push.
    }

    @Override
    public boolean shouldSkipMergeRequestBuild(GitLabPushTrigger trigger, String state) {
        String[] skipStates = { "closed", "merged" };
        String[] openStates = { "opened", "update" };
        
        if (ArrayUtils.contains(skipStates, state)) {
            return true;
        } else if (ArrayUtils.contains(openStates, state) && "never".equals(trigger.getTriggerOpenMergeRequestOnPush())) {
            return true;
        }
        
        return false;
    }

    @Override
    public boolean isMergeBuildAlreadyBuilt(GitLabMergeRequest request, Run mergeBuild) {
        return true;
    }

    @Override
    public boolean skipsExistingPushBuildsMatchingBySHA1() {
        return true;
    }

}
