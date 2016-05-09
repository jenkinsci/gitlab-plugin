package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.ObjectAttributes;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.trigger.exception.NoRevisionToBuildException;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.handler.AbstractWebHookTriggerHandler;
import com.dabsquared.gitlabjenkins.util.BuildUtil;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.RevisionParameterAction;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Robin Müller
 */
class MergeRequestHookTriggerHandlerLegacyImpl extends AbstractMergeRequestHookTriggerHandler implements MergeRequestHookTriggerHandler {

    private static final Logger LOGGER = Logger.getLogger(MergeRequestHookTriggerHandlerLegacyImpl.class.getName());

    private final List<State> allowedStates;

    MergeRequestHookTriggerHandlerLegacyImpl(List<State> allowedStates) {
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
        return new RevisionParameterAction(retrieveRevisionToBuild(hook), retrieveUrIish(hook));
    }

    private String retrieveRevisionToBuild(MergeRequestHook hook) throws NoRevisionToBuildException {
        if (hook.getObjectAttributes() != null
                && hook.getObjectAttributes().getLastCommit() != null
                && hook.getObjectAttributes().getLastCommit().getId() != null) {

            return hook.getObjectAttributes().getLastCommit().getId();
        } else {
            throw new NoRevisionToBuildException();
        }
    }

    private boolean isLastCommitNotYetBuild(Job<?, ?> project, MergeRequestHook hook) {
        ObjectAttributes objectAttributes = hook.getObjectAttributes();
        if (objectAttributes != null && objectAttributes.getLastCommit() != null) {
            Run<?, ?> mergeBuild = BuildUtil.getBuildBySHA1(project, objectAttributes.getLastCommit().getId(), true);
            if (mergeBuild != null && StringUtils.equals(getTargetBranchFromBuild(mergeBuild), objectAttributes.getTargetBranch())) {
                LOGGER.log(Level.INFO, "Last commit in Merge Request has already been built in build #" + mergeBuild.getNumber());
                return false;
            }
        }
        return true;
    }

    private String getTargetBranchFromBuild(Run<?, ?> mergeBuild) {
        GitLabWebHookCause cause = mergeBuild.getCause(GitLabWebHookCause.class);
        return cause == null ? null : cause.getData().getTargetBranch();
    }
}
