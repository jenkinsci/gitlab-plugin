package com.dabsquared.gitlabjenkins.trigger.handler.note;

import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import hudson.model.Job;
import org.gitlab4j.api.webhook.NoteEvent;

/**
 * @author Robin Müller
 */
class NopNoteHookTriggerHandler implements NoteHookTriggerHandler {
    @Override
    public void handle(
            Job<?, ?> job,
            NoteEvent event,
            boolean ciSkip,
            BranchFilter branchFilter,
            MergeRequestLabelFilter mergeRequestLabelFilter) {
        // nothing to do
    }
}
