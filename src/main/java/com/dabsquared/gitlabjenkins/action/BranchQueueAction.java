package com.dabsquared.gitlabjenkins.action;

import hudson.Util;
import hudson.model.Action;
import hudson.model.InvisibleAction;
import hudson.model.Queue.QueueAction;
import java.io.Serializable;
import java.util.List;

/**
 * Allows the queue scheduler to compare source branches when deduping
 *
 * @author Seb Hopley
 */
public class BranchQueueAction extends InvisibleAction implements Serializable, QueueAction {

    private String sourceBranch;

    public BranchQueueAction(String sourceBranch) {
        this.sourceBranch = sourceBranch;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    @Override
    public boolean shouldSchedule(List<Action> actions) {
        // this shouldn't happen so just return true
        if (this.sourceBranch == null) {
            return true;
        }
        List<BranchQueueAction> otherActions = Util.filter(actions, BranchQueueAction.class);
        for (BranchQueueAction action : otherActions) {
            if (this.sourceBranch.equals(action.getSourceBranch())) {
                return false;
            }
        }
        // if we get to this point there were no matching actions so a new build is required
        return true;
    }
}
