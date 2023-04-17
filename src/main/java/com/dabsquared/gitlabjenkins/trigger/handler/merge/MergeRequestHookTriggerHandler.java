package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.trigger.handler.WebHookTriggerHandler;

/**
 * @author Robin Müller
 */
public interface MergeRequestHookTriggerHandler extends WebHookTriggerHandler<MergeRequestHook> {}
