package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import hudson.model.Job;
import java.util.List;
import org.gitlab4j.api.systemhooks.PushSystemHookEvent;

class PushSystemHookTriggerHandlerList implements PushSystemHookTriggerHandler {

    private final List<PushSystemHookTriggerHandler> handlers;

    PushSystemHookTriggerHandlerList(List<PushSystemHookTriggerHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void handle(
            Job<?, ?> job,
            PushSystemHookEvent event,
            boolean ciSkip,
            BranchFilter branchFilter,
            MergeRequestLabelFilter mergeRequestLabelFilter) {
        for (PushSystemHookTriggerHandler handler : handlers) {
            handler.handle(job, event, ciSkip, branchFilter, mergeRequestLabelFilter);
        }
    }
}
