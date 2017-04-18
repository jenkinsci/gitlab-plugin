package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Robin Müller
 */
public final class MergeRequestHookTriggerHandlerFactory {

    private MergeRequestHookTriggerHandlerFactory() {}

    public static MergeRequestHookTriggerHandler newMergeRequestHookTriggerHandler(boolean triggerOnMergeRequest,
    		                                                                       boolean triggerOnAcceptedMergeRequest,
    		                                                                       boolean triggerOnClosedMergeRequest,
                                                                                   TriggerOpenMergeRequest triggerOpenMergeRequest,
                                                                                   boolean skipWorkInProgressMergeRequest) {
        if (triggerOnMergeRequest || triggerOnAcceptedMergeRequest || triggerOnClosedMergeRequest || triggerOpenMergeRequest != TriggerOpenMergeRequest.never) {
            return new MergeRequestHookTriggerHandlerImpl(retrieveAllowedStates(triggerOnMergeRequest, triggerOnAcceptedMergeRequest, triggerOnClosedMergeRequest, triggerOpenMergeRequest),
                                                          skipWorkInProgressMergeRequest);
        } else {
            return new NopMergeRequestHookTriggerHandler();
        }
    }

	private static List<State> retrieveAllowedStates(boolean triggerOnMergeRequest, 
			                                         boolean triggerOnAcceptedMergeRequest, 
			                                         boolean triggerOnClosedMergeRequest,
			                                         TriggerOpenMergeRequest triggerOpenMergeRequest) {
        List<State> result = new ArrayList<>();
        if (triggerOnMergeRequest) {
            result.add(State.opened);
            result.add(State.reopened);
        }
        if (triggerOnAcceptedMergeRequest)  {
        	result.add(State.merged);
        }
        if (triggerOnClosedMergeRequest) {
        	result.add(State.closed);
        }
        if (triggerOpenMergeRequest != TriggerOpenMergeRequest.never) {
            result.add(State.updated);
        }
        return result;
    }
}
