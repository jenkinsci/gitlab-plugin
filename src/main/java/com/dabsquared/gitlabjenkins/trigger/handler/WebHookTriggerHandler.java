package com.dabsquared.gitlabjenkins.trigger.handler;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.WebHook;
import com.dabsquared.gitlabjenkins.trigger.filter.Filter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
public interface WebHookTriggerHandler<H extends WebHook> {

    void handle(Job<?, ?> job, H hook, boolean ciSkip, Filter fileFilter, Filter branchFilter, MergeRequestLabelFilter mergeRequestLabelFilter);
}
