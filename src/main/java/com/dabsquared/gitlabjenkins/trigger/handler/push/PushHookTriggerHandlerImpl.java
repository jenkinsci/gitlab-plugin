package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.cause.GitLabPushCause;
import com.dabsquared.gitlabjenkins.model.Commit;
import com.dabsquared.gitlabjenkins.model.PushHook;
import com.dabsquared.gitlabjenkins.trigger.exception.NoRevisionToBuildException;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.plugins.git.RevisionParameterAction;
import jenkins.model.ParameterizedJobMixIn;
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
class PushHookTriggerHandlerImpl implements PushHookTriggerHandler {

    private final static Logger LOGGER = Logger.getLogger(PushHookTriggerHandlerImpl.class.getName());

    @Override
    public void handle(PushHookTriggerConfig config, Job<?, ?> job, PushHook hook) {
        if (config.getCiSkip() && isLastCommitCiSkip(hook)) {
            LOGGER.log(Level.INFO, "Skipping due to ci-skip.");
            return;
        }

        if (config.getBranchFilter().isBranchAllowed(hook.getRef().replaceFirst("^refs/heads/", ""))) {
            LOGGER.log(Level.INFO, "{0} triggered for push.", job.getFullName());
            scheduleBuild(job, createActions(job, hook));
        }
    }

    @Override
    public boolean isTriggerOnPush() {
        return true;
    }

    private Action[] createActions(Job<?, ?> job, PushHook hook) {
        ArrayList<Action> actions = new ArrayList<Action>();
        actions.add(new CauseAction(createGitLabPushCause(job, hook)));
        try {
            actions.add(createPushRequestRevisionParameter(hook));
        } catch (NoRevisionToBuildException e) {
            LOGGER.log(Level.WARNING, "unknown handled situation, dont know what revision to build for req {0} for job {1}",
                    new Object[]{hook, (job != null ? job.getFullName() : null)});
        }
        return actions.toArray(new Action[actions.size()]);
    }

    private GitLabPushCause createGitLabPushCause(Job<?, ?> job, PushHook hook) {
        try {
            return new GitLabPushCause(hook, new File(job.getRootDir(), "gitlab-polling.log"));
        } catch (IOException ex) {
            return new GitLabPushCause(hook);
        }
    }

    RevisionParameterAction createPushRequestRevisionParameter(PushHook hook) throws NoRevisionToBuildException {
        return new RevisionParameterAction(retrieveRevisionToBuild(hook), retrieveUrIish(hook));
    }

    private String retrieveRevisionToBuild(PushHook hook) throws NoRevisionToBuildException {
        if (hook.getCommits().isEmpty()) {
            if (isNewBranchPush(hook)) {
                return hook.getAfter();
            } else {
                throw new NoRevisionToBuildException();
            }
        } else {
            List<Commit> commits = hook.getCommits();
            return commits.get(commits.size() - 1).getId();
        }
    }

    private URIish retrieveUrIish(PushHook hook) {
        try {
            return new URIish(hook.getRepository().getUrl());
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "could not parse URL");
            return null;
        }
    }

    private boolean isNewBranchPush(PushHook pushHook) {
        return pushHook.getBefore() != null && pushHook.getBefore().contains("0000000000000000000000000000000000000000");
    }


    private void scheduleBuild(Job<?, ?> job, Action[] actions) {
        int projectBuildDelay = 0;
        if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
            ParameterizedJobMixIn.ParameterizedJob abstractProject = (ParameterizedJobMixIn.ParameterizedJob) job;
            if (abstractProject.getQuietPeriod() > projectBuildDelay) {
                projectBuildDelay = abstractProject.getQuietPeriod();
            }
        }
        retrieveScheduleJob(job).scheduleBuild2(projectBuildDelay, actions);
    }

    private ParameterizedJobMixIn retrieveScheduleJob(final Job<?, ?> job) {
        // TODO 1.621+ use standard method
        return new ParameterizedJobMixIn() {
            @Override
            protected Job asJob() {
                return job;
            }
        };
    }

    private boolean isLastCommitCiSkip(PushHook hook) {
        return !hook.getCommits().isEmpty() && hook.getCommits().get(0).getMessage().contains("[ci-skip]");
    }
}
