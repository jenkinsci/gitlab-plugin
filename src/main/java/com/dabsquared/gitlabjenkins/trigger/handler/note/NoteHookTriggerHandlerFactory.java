package com.dabsquared.gitlabjenkins.trigger.handler.note;

/**
 * @author Nikolay Ustinov
 */
public final class NoteHookTriggerHandlerFactory {

    private NoteHookTriggerHandlerFactory() {}

    public static NoteHookTriggerHandler newNoteHookTriggerHandler(boolean triggerOnNoteRequest, String noteRegex, boolean alwaysBuildHead) {
        if (triggerOnNoteRequest) {
            return new NoteHookTriggerHandlerImpl(noteRegex, alwaysBuildHead);
        } else {
            return new NopNoteHookTriggerHandler();
        }
    }
}
