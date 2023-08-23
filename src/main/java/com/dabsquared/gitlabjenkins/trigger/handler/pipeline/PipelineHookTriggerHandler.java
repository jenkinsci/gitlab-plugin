package com.dabsquared.gitlabjenkins.trigger.handler.pipeline;

import com.dabsquared.gitlabjenkins.trigger.handler.WebHookTriggerHandler;
import org.gitlab4j.api.webhook.PipelineEvent;

/**
 * @author Milena Zachow
 */
public interface PipelineHookTriggerHandler extends WebHookTriggerHandler<PipelineEvent> {}
