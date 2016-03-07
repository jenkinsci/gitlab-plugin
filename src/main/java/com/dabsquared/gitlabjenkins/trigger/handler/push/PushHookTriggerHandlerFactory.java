package com.dabsquared.gitlabjenkins.trigger.handler.push;

/**
 * @author Robin Müller
 */
public final class PushHookTriggerHandlerFactory {

    private PushHookTriggerHandlerFactory() {}

    public static PushHookTriggerHandler newPushHookTriggerHandler(boolean triggerOnPush) {
        if (triggerOnPush) {
            return new PushHookTriggerHandlerImpl();
        } else {
            return new NopPushHookTriggerHandler();
        }
    }
}
