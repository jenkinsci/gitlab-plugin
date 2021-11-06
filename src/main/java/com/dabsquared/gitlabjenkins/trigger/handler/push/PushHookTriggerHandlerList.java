package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.UserNameFilter;
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
    public void handle(Job<?, ?> job, PushHook hook, boolean ciSkip, BranchFilter branchFilter, MergeRequestLabelFilter mergeRequestLabelFilter, UserNameFilter userNameFilter) {
        for (PushHookTriggerHandler handler : handlers) {
            handler.handle(job, hook, ciSkip, branchFilter, mergeRequestLabelFilter, userNameFilter);
        }
    }
}
