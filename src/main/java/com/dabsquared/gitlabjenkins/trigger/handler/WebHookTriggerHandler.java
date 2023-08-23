package com.dabsquared.gitlabjenkins.trigger.handler;

import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
public interface WebHookTriggerHandler<E> {

    void handle(
            Job<?, ?> job,
            E event,
            boolean ciSkip,
            BranchFilter branchFilter,
            MergeRequestLabelFilter mergeRequestLabelFilter);
}
