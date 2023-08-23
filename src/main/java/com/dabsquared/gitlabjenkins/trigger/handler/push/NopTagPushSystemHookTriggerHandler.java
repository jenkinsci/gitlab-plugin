package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import hudson.model.Job;
import org.gitlab4j.api.systemhooks.TagPushSystemHookEvent;

/**
 * @author Robin Müller
 */
class NopTagPushSystemHookTriggerHandler implements TagPushSystemHookTriggerHandler {
    @Override
    public void handle(
            Job<?, ?> job,
            TagPushSystemHookEvent event,
            boolean ciSkip,
            BranchFilter branchFilter,
            MergeRequestLabelFilter mergeRequestLabelFilter) {
        // nothing to do
    }
}
