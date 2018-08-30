package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.Action;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;

import static com.dabsquared.gitlabjenkins.trigger.handler.merge.StateAndActionConfig.notEqual;
import static com.dabsquared.gitlabjenkins.trigger.handler.merge.StateAndActionConfig.nullOrContains;
import static java.util.EnumSet.of;

/**
 * @author Robin Müller
 */
public final class MergeRequestHookTriggerHandlerFactory {

    private MergeRequestHookTriggerHandlerFactory() {}

    public static MergeRequestHookTriggerHandler newMergeRequestHookTriggerHandler(boolean triggerOnMergeRequest,
    		                                                                       boolean triggerOnAcceptedMergeRequest,
    		                                                                       boolean triggerOnClosedMergeRequest,
                                                                                   TriggerOpenMergeRequest triggerOpenMergeRequest,
                                                                                   boolean skipWorkInProgressMergeRequest,
                                                                                   boolean triggerOnApprovedMergeRequest,
                                                                                   boolean cancelPendingBuildsOnUpdate) {

        TriggerConfigChain chain = new TriggerConfigChain();
        chain
            .acceptOnlyIf(triggerOnApprovedMergeRequest, null, of(Action.approved))
            .acceptIf(triggerOnMergeRequest, of(State.opened, State.reopened), null)
            .acceptIf(triggerOnAcceptedMergeRequest, null, of(Action.merge))
            .acceptIf(triggerOnClosedMergeRequest, null, of(Action.closed))
            .acceptIf(triggerOpenMergeRequest != TriggerOpenMergeRequest.never, of(State.updated), null)
        ;

        return new MergeRequestHookTriggerHandlerImpl(chain, skipWorkInProgressMergeRequest, cancelPendingBuildsOnUpdate);
    }

}
