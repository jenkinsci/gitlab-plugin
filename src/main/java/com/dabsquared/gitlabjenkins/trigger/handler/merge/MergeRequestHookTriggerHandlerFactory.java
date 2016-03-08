package com.dabsquared.gitlabjenkins.trigger.handler.merge;

/**
 * @author Robin Müller
 */
public final class MergeRequestHookTriggerHandlerFactory {

    private MergeRequestHookTriggerHandlerFactory() {}

    public static MergeRequestHookTriggerHandler newMergeRequestHookTriggerHandler(boolean triggerOnMergeRequest) {
        if (triggerOnMergeRequest) {
            return new MergeRequestHookTriggerHandlerImpl();
        } else {
            return new NopMergeRequestHookTriggerHandler();
        }
    }
}
