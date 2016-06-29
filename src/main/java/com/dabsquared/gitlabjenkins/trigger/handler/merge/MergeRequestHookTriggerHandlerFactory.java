package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.GitLabPluginMode;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Robin Müller
 */
public final class MergeRequestHookTriggerHandlerFactory {

    private MergeRequestHookTriggerHandlerFactory() {}

    public static MergeRequestHookTriggerHandler newMergeRequestHookTriggerHandler(GitLabPluginMode gitLabPluginMode,
                                                                                   boolean triggerOnMergeRequest,
                                                                                   TriggerOpenMergeRequest triggerOpenMergeRequest,
                                                                                   boolean skipWorkInProgressMergeRequest) {
        if (triggerOnMergeRequest || triggerOpenMergeRequest != TriggerOpenMergeRequest.never) {
            if (gitLabPluginMode == GitLabPluginMode.LEGACY) {
                return new MergeRequestHookTriggerHandlerLegacyImpl(retrieveAllowedStates(triggerOnMergeRequest, triggerOpenMergeRequest),
                                                                    skipWorkInProgressMergeRequest);
            } else {
                return new MergeRequestHookTriggerHandlerModernImpl(retrieveAllowedStates(triggerOnMergeRequest, triggerOpenMergeRequest),
                                                                    skipWorkInProgressMergeRequest);
            }
        } else {
            return new NopMergeRequestHookTriggerHandler();
        }
    }

    private static List<State> retrieveAllowedStates(boolean triggerOnMergeRequest, TriggerOpenMergeRequest triggerOpenMergeRequest) {
        List<State> result = new ArrayList<>();
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
