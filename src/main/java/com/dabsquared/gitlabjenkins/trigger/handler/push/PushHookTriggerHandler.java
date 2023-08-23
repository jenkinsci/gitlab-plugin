package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.trigger.handler.WebHookTriggerHandler;
import org.gitlab4j.api.webhook.PushEvent;

/**
 * @author Robin Müller
 */
public interface PushHookTriggerHandler extends WebHookTriggerHandler<PushEvent> {}
