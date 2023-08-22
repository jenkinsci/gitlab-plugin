package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import hudson.model.Job;
import org.gitlab4j.api.webhook.TagPushEvent;

/**
 * @author Robin Müller
 */
class NopTagPushHookTriggerHandler implements TagPushHookTriggerHandler {

    @Override
    public void handle(
            Job<?, ?> job,
            TagPushEvent event,
            boolean ciSkip,
            BranchFilter branchFilter,
            MergeRequestLabelFilter mergeRequestLabelFilter) {
        // nothing to do
    }
}
