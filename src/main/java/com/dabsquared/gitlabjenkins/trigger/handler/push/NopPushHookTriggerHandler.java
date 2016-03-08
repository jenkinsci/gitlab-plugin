package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.model.PushHook;
import com.dabsquared.gitlabjenkins.trigger.handler.WebHookTriggerConfig;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
class NopPushHookTriggerHandler implements PushHookTriggerHandler {
    @Override
    public void handle(WebHookTriggerConfig config, Job<?, ?> job, PushHook hook) {
        // do nothing
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
