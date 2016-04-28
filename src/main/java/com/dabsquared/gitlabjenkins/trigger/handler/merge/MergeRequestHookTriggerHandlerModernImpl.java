package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.ObjectAttributes;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.trigger.exception.NoRevisionToBuildException;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.util.BuildUtil;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.RevisionParameterAction;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Robin Müller
 */
class MergeRequestHookTriggerHandlerModernImpl extends AbstractMergeRequestHookTriggerHandler implements MergeRequestHookTriggerHandler {

    private static final Logger LOGGER = Logger.getLogger(MergeRequestHookTriggerHandlerModernImpl.class.getName());

    private final List<State> allowedStates;

    MergeRequestHookTriggerHandlerModernImpl(List<State> allowedStates) {
        this.allowedStates = allowedStates;
    }

    @Override
    public void handle(Job<?, ?> job, MergeRequestHook hook, boolean ciSkip, BranchFilter branchFilter) {
        if (allowedStates.contains(hook.getObjectAttributes().getState()) && isLastCommitNotYetBuild(job, hook)) {
            super.handle(job, hook, ciSkip, branchFilter);
        }
    }

    @Override
    protected boolean isCiSkip(MergeRequestHook hook) {
        return hook.getObjectAttributes() != null
                && hook.getObjectAttributes().getDescription() != null
                && hook.getObjectAttributes().getDescription().contains("[ci-skip]");
    }

    @Override
    protected String getTargetBranch(MergeRequestHook hook) {
        return hook.getObjectAttributes() == null ? null : hook.getObjectAttributes().getTargetBranch();
    }

    @Override
    protected String getTriggerType() {
        return "merge request";
    }

    @Override
    protected RevisionParameterAction createRevisionParameter(MergeRequestHook hook) throws NoRevisionToBuildException {
        return new RevisionParameterAction(String.format("refs/remotes/origin/merge-requests/%s", hook.getObjectAttributes().getIid()), retrieveUrIish(hook));
    }

    private boolean isLastCommitNotYetBuild(Job<?, ?> project, MergeRequestHook hook) {
        ObjectAttributes objectAttributes = hook.getObjectAttributes();
        if (objectAttributes != null && objectAttributes.getLastCommit() != null) {
            Run<?, ?> mergeBuild = BuildUtil.getBuildBySHA1(project, objectAttributes.getLastCommit().getId(), true);
            if (mergeBuild != null) {
                LOGGER.log(Level.INFO, "Last commit in Merge Request has already been built in build #" + mergeBuild.getNumber());
                return false;
            }
        }
        return true;
    }
}
