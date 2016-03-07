package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.model.PushHook;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
public interface PushHookTriggerHandler {

    void handle(PushHookTriggerConfig config, Job<?, ?> job, PushHook hook);

    boolean isTriggerOnPush();
}
