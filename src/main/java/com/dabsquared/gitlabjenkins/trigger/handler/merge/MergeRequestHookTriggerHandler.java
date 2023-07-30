package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.trigger.handler.WebHookTriggerHandler;
import org.gitlab4j.api.webhook.MergeRequestEvent;

/**
 * @author Robin Müller
 */
public interface MergeRequestHookTriggerHandler extends WebHookTriggerHandler<MergeRequestEvent> {}
