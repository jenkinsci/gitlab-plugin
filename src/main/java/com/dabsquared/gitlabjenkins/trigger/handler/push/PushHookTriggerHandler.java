package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import com.dabsquared.gitlabjenkins.trigger.handler.WebHookTriggerHandler;

/**
 * @author Robin Müller
 */
public interface PushHookTriggerHandler extends WebHookTriggerHandler<PushHook> {}
