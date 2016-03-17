package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.cause.GitLabMergeCause;
import com.dabsquared.gitlabjenkins.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.model.ObjectAttributes;
import com.dabsquared.gitlabjenkins.model.State;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.handler.AbstractWebHookTriggerHandler;
import com.dabsquared.gitlabjenkins.util.BuildUtil;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.model.Run;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Robin Müller
 */
class MergeRequestHookTriggerHandlerImpl extends AbstractWebHookTriggerHandler<MergeRequestHook> implements MergeRequestHookTriggerHandler {

    private static final Logger LOGGER = Logger.getLogger(MergeRequestHookTriggerHandlerImpl.class.getName());

    private final List<State> allowedStates;

    MergeRequestHookTriggerHandlerImpl(List<State> allowedStates) {
        this.allowedStates = allowedStates;
    }

    @Override
    public void handle(Job<?, ?> job, MergeRequestHook hook, boolean ciSkip, BranchFilter branchFilter) {
        if (allowedStates.contains(hook.getObjectAttributes().optState().orNull()) && isLastCommitNotYetBuild(job, hook)) {
            super.handle(job, hook, ciSkip, branchFilter);
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    protected boolean isCiSkip(MergeRequestHook hook) {
        return hook.getObjectAttributes().optDescription().or("").contains("[ci-skip]");
    }

    @Override
    protected Action[] createActions(Job<?, ?> job, MergeRequestHook hook) {
        return new Action[] { new CauseAction(createGitLabMergeCause(job, hook)) };
    }

    @Override
    protected String getTargetBranch(MergeRequestHook hook) {
        return hook.getObjectAttributes().optTargetBranch().orNull();
    }

    @Override
    protected String getTriggerType() {
        return "merge request";
    }

    private GitLabMergeCause createGitLabMergeCause(Job<?, ?> job, MergeRequestHook mergeRequestHook) {
        try {
            return new GitLabMergeCause(mergeRequestHook, new File(job.getRootDir(), "gitlab-polling.log"));
        } catch (IOException ex) {
            return new GitLabMergeCause(mergeRequestHook);
        }
    }

    private boolean isLastCommitNotYetBuild(Job<?, ?> project, MergeRequestHook hook) {
        ObjectAttributes objectAttributes = hook.getObjectAttributes();
        if (objectAttributes.optLastCommit().isPresent()) {
            Run<?, ?> mergeBuild = BuildUtil.getBuildBySHA1(project, objectAttributes.optLastCommit().get().optId().get(), true);
            if (mergeBuild != null && StringUtils.equals(getTargetBranchFromBuild(mergeBuild), objectAttributes.optTargetBranch().get())) {
                LOGGER.log(Level.INFO, "Last commit in Merge Request has already been built in build #" + mergeBuild.getNumber());
                return false;
            }
        }
        return true;
    }

    private String getTargetBranchFromBuild(Run<?, ?> mergeBuild) {
        GitLabMergeCause cause = mergeBuild.getCause(GitLabMergeCause.class);
        return cause == null ? null : cause.getTargetBranch();
    }
}
