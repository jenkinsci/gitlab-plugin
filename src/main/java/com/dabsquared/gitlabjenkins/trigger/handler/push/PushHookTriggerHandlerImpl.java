package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.cause.GitLabPushCause;
import com.dabsquared.gitlabjenkins.model.Commit;
import com.dabsquared.gitlabjenkins.model.PushHook;
import com.dabsquared.gitlabjenkins.trigger.exception.NoRevisionToBuildException;
import com.dabsquared.gitlabjenkins.trigger.handler.AbstractWebHookTriggerHandler;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.plugins.git.RevisionParameterAction;
import org.eclipse.jgit.transport.URIish;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Robin Müller
 */
class PushHookTriggerHandlerImpl extends AbstractWebHookTriggerHandler<PushHook> implements PushHookTriggerHandler {

    private final static Logger LOGGER = Logger.getLogger(PushHookTriggerHandlerImpl.class.getName());

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    protected boolean isCiSkip(PushHook hook) {
        List<Commit> commits = hook.getCommits();
        return !commits.isEmpty() && commits.get(0).optMessage().or("").contains("[ci-skip]");
    }

    @Override
    protected CauseAction createCauseAction(Job<?, ?> job, PushHook hook) {
        return new CauseAction(createGitLabPushCause(job, hook));
    }

    @Override
    protected String getTargetBranch(PushHook hook) {
        return hook.optRef().or("").replaceFirst("^refs/heads/", "");
    }

    @Override
    protected String getTriggerType() {
        return "push";
    }

    private GitLabPushCause createGitLabPushCause(Job<?, ?> job, PushHook hook) {
        try {
            return new GitLabPushCause(hook, new File(job.getRootDir(), "gitlab-polling.log"));
        } catch (IOException ex) {
            return new GitLabPushCause(hook);
        }
    }

    @Override
    protected RevisionParameterAction createRevisionParameter(PushHook hook) throws NoRevisionToBuildException {
        return new RevisionParameterAction(retrieveRevisionToBuild(hook), retrieveUrIish(hook));
    }

    private String retrieveRevisionToBuild(PushHook hook) throws NoRevisionToBuildException {
        if (hook.getCommits().isEmpty()) {
            if (isNewBranchPush(hook)) {
                return hook.optAfter().orNull();
            } else {
                throw new NoRevisionToBuildException();
            }
        } else {
            List<Commit> commits = hook.getCommits();
            return commits.get(commits.size() - 1).optId().orNull();
        }
    }

    private URIish retrieveUrIish(PushHook hook) {
        try {
            return new URIish(hook.getRepository().optUrl().orNull());
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "could not parse URL");
            return null;
        }
    }

    private boolean isNewBranchPush(PushHook pushHook) {
        return pushHook.optBefore().or("").contains("0000000000000000000000000000000000000000");
    }
}
