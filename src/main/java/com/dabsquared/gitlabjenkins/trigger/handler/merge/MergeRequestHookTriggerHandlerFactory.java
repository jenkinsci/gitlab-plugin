package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.Action;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
        chain.addIf(triggerOnMergeRequest, nullOrContains(of(State.opened, State.reopened)), notEqual(Action.approved))
            .addIf(triggerOnAcceptedMergeRequest, null, of(Action.merge))
            .addIf(triggerOnClosedMergeRequest, null, of(Action.closed))
            .addIf(triggerOpenMergeRequest != TriggerOpenMergeRequest.never, of(State.updated), null)
            .addIf(triggerOnApprovedMergeRequest, null, of(Action.approved))
        ;

        return new MergeRequestHookTriggerHandlerImpl(chain, skipWorkInProgressMergeRequest, cancelPendingBuildsOnUpdate);
    }

}
