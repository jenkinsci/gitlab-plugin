package com.dabsquared.gitlabjenkins.trigger.handler;

import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
// when extending AbstractEvent, pushhook wont work as it is not a subclass of AbstractEvent. Push event has a seperate
// parent class AbstractPushEvent
// the problem is that events are distributed among 2 parent event classes (AbstractEvent doesnt have getRepository
// whereas AbstractPushEvent has it). we cant put them in one umbrella i.e Abstrac
public interface WebHookTriggerHandler<E> {

    void handle(
            Job<?, ?> job,
            E event,
            boolean ciSkip,
            BranchFilter branchFilter,
            MergeRequestLabelFilter mergeRequestLabelFilter);
}
