package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.cause.GitLabMergeCause;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.api.model.ObjectAttributes;
import com.dabsquared.gitlabjenkins.gitlab.api.model.State;
import com.dabsquared.gitlabjenkins.trigger.exception.NoRevisionToBuildException;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.handler.AbstractWebHookTriggerHandler;
import com.dabsquared.gitlabjenkins.util.BuildUtil;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.RevisionParameterAction;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.URIish;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
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
    protected String getTargetBranch(MergeRequestHook hook) {
        return hook.getObjectAttributes().optTargetBranch().orNull();
    }

    @Override
    protected String getTriggerType() {
        return "merge request";
    }

    @Override
    protected CauseAction createCauseAction(Job<?, ?> job, MergeRequestHook hook) {
        return new CauseAction(createGitLabMergeCause(job, hook));
    }

    @Override
    protected RevisionParameterAction createRevisionParameter(MergeRequestHook hook) throws NoRevisionToBuildException {
        return new RevisionParameterAction(retrieveRevisionToBuild(hook), retrieveUrIish(hook));
    }

    private GitLabMergeCause createGitLabMergeCause(Job<?, ?> job, MergeRequestHook mergeRequestHook) {
        try {
            return new GitLabMergeCause(mergeRequestHook, new File(job.getRootDir(), "gitlab-polling.log"));
        } catch (IOException ex) {
            return new GitLabMergeCause(mergeRequestHook);
        }
    }

    private String retrieveRevisionToBuild(MergeRequestHook hook) throws NoRevisionToBuildException {
        if (hook.getObjectAttributes().getLastCommit().optId().isPresent()) {
            return hook.getObjectAttributes().getLastCommit().optId().get();
        } else {
            throw new NoRevisionToBuildException();
        }
    }

    private URIish retrieveUrIish(MergeRequestHook hook) {
        try {
            return new URIish(hook.getRepository().optUrl().orNull());
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "could not parse URL");
            return null;
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
