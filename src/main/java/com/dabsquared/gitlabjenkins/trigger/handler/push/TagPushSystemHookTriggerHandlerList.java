package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import hudson.model.Job;
import java.util.List;
import org.gitlab4j.api.systemhooks.TagPushSystemHookEvent;

class TagPushSystemHookTriggerHandlerList implements TagPushSystemHookTriggerHandler {

    private final List<TagPushSystemHookTriggerHandler> handlers;

    TagPushSystemHookTriggerHandlerList(List<TagPushSystemHookTriggerHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void handle(
            Job<?, ?> job,
            TagPushSystemHookEvent event,
            boolean ciSkip,
            BranchFilter branchFilter,
            MergeRequestLabelFilter mergeRequestLabelFilter) {
        for (TagPushSystemHookTriggerHandler handler : handlers) {
            handler.handle(job, event, ciSkip, branchFilter, mergeRequestLabelFilter);
        }
    }
}
