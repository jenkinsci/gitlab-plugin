package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
class NopMergeRequestHookTriggerHandler implements MergeRequestHookTriggerHandler {
    @Override
    public void handle(Job<?, ?> job, MergeRequestHook hook, boolean ciSkip, BranchFilter branchFilter) {
        // nothing to do
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
