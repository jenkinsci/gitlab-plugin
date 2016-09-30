package com.dabsquared.gitlabjenkins.trigger;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jgit.transport.URIish;

import hudson.Util;
import hudson.model.Action;
import hudson.model.InvisibleAction;
import hudson.model.Queue;
import hudson.model.Queue.QueueAction;
import hudson.model.queue.FoldableAction;

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

}
