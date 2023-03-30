package com.dabsquared.gitlabjenkins.trigger.handler.pipeline;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.PipelineHook;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import hudson.model.Job;

/**
 * @author Milena Zachow
 */
class NopPipelineHookTriggerHandler implements PipelineHookTriggerHandler {

    @Override
    public void handle(Job<?, ?> job, PipelineHook hook, boolean ciSkip, BranchFilter branchFilter, MergeRequestLabelFilter mergeRequestLabelFilter) {

    }
}
