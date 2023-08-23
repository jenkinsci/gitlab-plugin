package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import hudson.model.Job;
import java.util.List;
import org.gitlab4j.api.webhook.PushEvent;

/**
 * @author Robin Müller
 */
class PushHookTriggerHandlerList implements PushHookTriggerHandler {

    private final List<PushHookTriggerHandler> handlers;

    PushHookTriggerHandlerList(List<PushHookTriggerHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void handle(
            Job<?, ?> job,
            PushEvent event,
            boolean ciSkip,
            BranchFilter branchFilter,
            MergeRequestLabelFilter mergeRequestLabelFilter) {
        for (PushHookTriggerHandler handler : handlers) {
            handler.handle(job, event, ciSkip, branchFilter, mergeRequestLabelFilter);
        }
    }
}
