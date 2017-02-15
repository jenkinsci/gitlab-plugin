package com.dabsquared.gitlabjenkins.trigger;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import com.dabsquared.gitlabjenkins.Messages;

import hudson.Util;
import hudson.model.Action;
import hudson.model.InvisibleAction;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Queue.QueueAction;
import hudson.model.queue.FoldableAction;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import jenkins.triggers.SCMTriggerItem;

/**
 * Used as a build parameter to specify the revision to be built.
 */
public class WebHookRevisionParameterAction extends InvisibleAction
        implements Serializable, QueueAction, FoldableAction {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(WebHookRevisionParameterAction.class.getName());

    private final URIish repoURL;

    /**
     * SHA1, branch, tag, etc. to get the source to build.
     */
    private final String sourceCommit;

    /**
     * SHA1, branch, tag, etc. where the source is integrated
     */
    private final String targetCommit;

    public WebHookRevisionParameterAction(URIish repoURL, String commit) {
        this(repoURL, commit, commit);
    }

    public WebHookRevisionParameterAction(URIish repoURL, String sourceCommit, String targetCommit) {
        this.repoURL = repoURL;
        this.sourceCommit = sourceCommit;
        this.targetCommit = targetCommit;
    }

    public URIish getRepoURI() {
        return repoURL;
    }

    public String getSourceCommit() {
        return sourceCommit;
    }

    public boolean shouldSchedule(List<Action> actions) {
        List<WebHookRevisionParameterAction> otherActions = Util.filter(actions, WebHookRevisionParameterAction.class);
        for (WebHookRevisionParameterAction action : otherActions) {
            if (this.sourceCommit.equals(action.sourceCommit) && this.targetCommit.equals(action.targetCommit))
                return false;
        }
        return true;
    }

    public void foldIntoExisting(Queue.Item item, Queue.Task owner, List<Action> otherActions) {
        // not supported
    }

    /**
     * Get the URL of the first declared repository in the project configuration.
     * Use this as default source repository url.
     *
     * @return URIish the default value of the source repository url
     * @throws IllegalStateException Project does not use git scm.
     */
    public static URIish getSourceRepoURLDefault(Job<?, ?> job) {
        List<WebHookRevisionParameterAction> actions = job.getActions(WebHookRevisionParameterAction.class);
        if (actions.isEmpty()) {
            // fallback to git-plugin
            return getSourceRepoURLDefaultFromSCM(job);
        }
        return getFirstRepoURL(actions);
    }

    private static URIish getFirstRepoURL(List<WebHookRevisionParameterAction> actions) {
        if (!actions.isEmpty()) {
            WebHookRevisionParameterAction action = actions.get(actions.size() - 1);
            return action.getRepoURI();
        }
        throw new IllegalStateException(Messages.GitLabPushTrigger_NoSourceRepository());
    }
    
    /**
     * Get the URL of the first declared repository in the project configuration.
     * Use this as default source repository url.
     *
     * @return URIish the default value of the source repository url
     * @throws IllegalStateException Project does not use git scm.
     */
    private static URIish getSourceRepoURLDefaultFromSCM(Job<?, ?> job) {
        SCMTriggerItem item = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(job);
        GitSCM gitSCM = getGitSCM(item);
        if (gitSCM == null) {
            LOGGER.log(Level.WARNING, "Could not find GitSCM for project. Project = {1}, next build = {2}",
                    array(job.getName(), String.valueOf(job.getNextBuildNumber())));
            throw new IllegalStateException("This project does not use git:" + job.getName());
        }
        return getFirstRepoURLFromSCM(gitSCM.getRepositories());
    }

    private static URIish getFirstRepoURLFromSCM(List<RemoteConfig> repositories) {
        if (!repositories.isEmpty()) {
            List<URIish> uris = repositories.get(repositories.size() - 1).getURIs();
            if (!uris.isEmpty()) {
                return uris.get(uris.size() - 1);
            }
        }
        throw new IllegalStateException(Messages.GitLabPushTrigger_NoSourceRepository());
    }

    private static GitSCM getGitSCM(SCMTriggerItem item) {
        if (item != null) {
            for (SCM scm : item.getSCMs()) {
                if (scm instanceof GitSCM) {
                    return (GitSCM) scm;
                }
            }
        }
        return null;
    }
    
    private static Object[] array(Object... objects) {
        return objects;
    }

}
