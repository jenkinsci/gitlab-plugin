package com.dabsquared.gitlabjenkins.trigger.handler;

import com.dabsquared.gitlabjenkins.model.WebHook;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
public interface WebHookTriggerHandler<H extends WebHook> {

    void handle(WebHookTriggerConfig config, Job<?, ?> job, H hook);

    boolean isEnabled();
}
