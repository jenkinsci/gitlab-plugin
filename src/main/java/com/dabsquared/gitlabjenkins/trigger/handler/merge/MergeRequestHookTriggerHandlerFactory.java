package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.gitlab.api.model.State;
import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Robin Müller
 */
public final class MergeRequestHookTriggerHandlerFactory {

    private MergeRequestHookTriggerHandlerFactory() {}

    public static MergeRequestHookTriggerHandler newMergeRequestHookTriggerHandler(boolean triggerOnMergeRequest, TriggerOpenMergeRequest triggerOpenMergeRequest) {
        if (triggerOnMergeRequest || triggerOpenMergeRequest != TriggerOpenMergeRequest.never) {
            return new MergeRequestHookTriggerHandlerImpl(retrieveAllowedStates(triggerOnMergeRequest, triggerOpenMergeRequest));
        } else {
            return new NopMergeRequestHookTriggerHandler();
        }
    }

    private static List<State> retrieveAllowedStates(boolean triggerOnMergeRequest, TriggerOpenMergeRequest triggerOpenMergeRequest) {
        List<State> result = new ArrayList<State>();
        if (triggerOnMergeRequest) {
            result.add(State.opened);
            result.add(State.reopened);
        }
        if (triggerOpenMergeRequest != TriggerOpenMergeRequest.never) {
            result.add(State.updated);
        }
        return result;
    }
}
