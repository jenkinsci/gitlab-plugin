package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import com.dabsquared.gitlabjenkins.trigger.filter.Filter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
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
    public void handle(Job<?, ?> job, PushHook hook, boolean ciSkip, Filter fileFilter, Filter branchFilter, MergeRequestLabelFilter mergeRequestLabelFilter) {
        for (PushHookTriggerHandler handler : handlers) {
            handler.handle(job, hook, ciSkip, fileFilter, branchFilter, mergeRequestLabelFilter);
        }
    }
}
