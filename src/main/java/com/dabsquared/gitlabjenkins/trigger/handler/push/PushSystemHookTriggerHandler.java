package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.trigger.handler.WebHookTriggerHandler;
import org.gitlab4j.api.systemhooks.PushSystemHookEvent;

public interface PushSystemHookTriggerHandler extends WebHookTriggerHandler<PushSystemHookEvent> {}
