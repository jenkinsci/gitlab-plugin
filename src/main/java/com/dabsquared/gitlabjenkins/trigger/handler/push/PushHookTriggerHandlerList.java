package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.model.PushHook;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import hudson.model.Job;

import java.util.List;

/**
 * @author Robin Müller
 */
class PushHookTriggerHandlerList implements PushHookTriggerHandler {

    private final List<PushHookTriggerHandler> handlers;

    PushHookTriggerHandlerList(List<PushHookTriggerHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void handle(Job<?, ?> job, PushHook hook, boolean ciSkip, BranchFilter branchFilter) {
        for (PushHookTriggerHandler handler : handlers) {
            handler.handle(job, hook, ciSkip, branchFilter);
        }
    }

    @Override
    public boolean isEnabled() {
        for (PushHookTriggerHandler handler : handlers) {
            if (handler.isEnabled()) {
                return true;
            }
        }
        return false;
    }
}
