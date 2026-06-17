package com.dabsquared.gitlabjenkins;

import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;

public interface MergeRequestTriggerConfig {
    boolean getTriggerOnMergeRequest();

    boolean isTriggerOnlyIfNewCommitsPushed();

    boolean isTriggerOnAcceptedMergeRequest();

    boolean isTriggerOnApprovedMergeRequest();

    boolean isTriggerOnClosedMergeRequest();

    TriggerOpenMergeRequest getTriggerOpenMergeRequestOnPush();

    boolean isSkipWorkInProgressMergeRequest();

    String getLabelsThatForcesBuildIfAdded();

    boolean getCancelPendingBuildsOnUpdate();

    /**
     * Whether running builds for the same merge request source branch should be aborted when the
     * merge request is updated. Independent of {@link #getCancelPendingBuildsOnUpdate()} — either
     * can be enabled on its own.
     */
    default boolean getCancelRunningBuildsOnUpdate() {
        return false;
    }
}
