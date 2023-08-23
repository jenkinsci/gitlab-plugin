package com.dabsquared.gitlabjenkins.trigger.handler.pipeline;

import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import hudson.model.Job;
import org.gitlab4j.api.webhook.PipelineEvent;

/**
 * @author Milena Zachow
 */
class NopPipelineHookTriggerHandler implements PipelineHookTriggerHandler {

    @Override
    public void handle(
            Job<?, ?> job,
            PipelineEvent event,
            boolean ciSkip,
            BranchFilter branchFilter,
            MergeRequestLabelFilter mergeRequestLabelFilter) {}
}
