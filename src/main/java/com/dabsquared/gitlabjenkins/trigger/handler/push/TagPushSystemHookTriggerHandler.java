package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.trigger.handler.WebHookTriggerHandler;
import org.gitlab4j.api.systemhooks.TagPushSystemHookEvent;

public interface TagPushSystemHookTriggerHandler extends WebHookTriggerHandler<TagPushSystemHookEvent> {}
