package com.dabsquared.gitlabjenkins.trigger.handler.pipeline;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.PipelineHook;
import com.dabsquared.gitlabjenkins.trigger.filter.Filter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import hudson.model.Job;

/**
 * @author Milena Zachow
 */
class NopPipelineHookTriggerHandler implements PipelineHookTriggerHandler {

    @Override
    public void handle(Job<?, ?> job, PipelineHook hook, boolean ciSkip, Filter fileFilter, Filter branchFilter, MergeRequestLabelFilter mergeRequestLabelFilter) {

    }
}
