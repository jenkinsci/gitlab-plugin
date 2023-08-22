package com.dabsquared.gitlabjenkins.trigger.handler.note;

import com.dabsquared.gitlabjenkins.trigger.handler.WebHookTriggerHandler;
import org.gitlab4j.api.webhook.NoteEvent;

/**
 * @author Nikolay Ustinov
 */
public interface NoteHookTriggerHandler extends WebHookTriggerHandler<NoteEvent> {}
