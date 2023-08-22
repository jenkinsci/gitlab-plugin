package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.trigger.handler.WebHookTriggerHandler;
import org.gitlab4j.api.webhook.TagPushEvent;

public interface TagPushHookTriggerHandler extends WebHookTriggerHandler<TagPushEvent> {}
