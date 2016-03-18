package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.gitlab.api.model.PushHook;
import com.dabsquared.gitlabjenkins.trigger.handler.WebHookTriggerHandler;

/**
 * @author Robin Müller
 */
public interface PushHookTriggerHandler extends WebHookTriggerHandler<PushHook> { }
