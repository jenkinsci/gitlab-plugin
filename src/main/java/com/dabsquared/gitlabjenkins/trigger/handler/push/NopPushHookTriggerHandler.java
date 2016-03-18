package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.gitlab.api.model.PushHook;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
class NopPushHookTriggerHandler implements PushHookTriggerHandler {
    @Override
    public void handle(Job<?, ?> job, PushHook hook, boolean ciSkip, BranchFilter branchFilter) {
        // nothing to do
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
