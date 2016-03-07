package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.model.PushHook;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
class NopPushHookTriggerHandler implements PushHookTriggerHandler {
    @Override
    public void handle(PushHookTriggerConfig config, Job<?, ?> job, PushHook hook) {
        // do nothing
    }

    @Override
    public boolean isTriggerOnPush() {
        return false;
    }
}
