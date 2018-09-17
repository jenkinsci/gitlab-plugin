package com.dabsquared.gitlabjenkins;

import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;

public interface MergeRequestTriggerConfig {
    boolean getTriggerOnMergeRequest();

    boolean isTriggerOnAcceptedMergeRequest();

    boolean isTriggerOnApprovedMergeRequest();

    boolean isTriggerOnClosedMergeRequest();

    TriggerOpenMergeRequest getTriggerOpenMergeRequestOnPush();

    boolean isSkipWorkInProgressMergeRequest();

    boolean getCancelPendingBuildsOnUpdate();
}
