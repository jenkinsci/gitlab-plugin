package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import hudson.model.Job;
import org.gitlab4j.api.systemhooks.PushSystemHookEvent;

/**
 * @author Robin Müller
 */
class NopPushSystemHookTriggerHandler implements PushSystemHookTriggerHandler {
    @Override
    public void handle(
            Job<?, ?> job,
            PushSystemHookEvent event,
            boolean ciSkip,
            BranchFilter branchFilter,
            MergeRequestLabelFilter mergeRequestLabelFilter) {
        // nothing to do
    }
}
