package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;
import java.util.ArrayList;
import java.util.List;

public final class PushSystemHookTriggerHandlerFactory {

    private PushSystemHookTriggerHandlerFactory() {}

    public static PushSystemHookTriggerHandler newPushHookTriggerHandler(
            boolean triggerOnPush,
            boolean triggerToBranchDeleteRequest,
            TriggerOpenMergeRequest triggerOpenMergeRequestOnPush,
            boolean skipWorkInProgressMergeRequest) {
        if (triggerOnPush || triggerOpenMergeRequestOnPush == TriggerOpenMergeRequest.both) {
            return new PushSystemHookTriggerHandlerList(retrieveHandlers(
                    triggerOnPush,
                    triggerToBranchDeleteRequest,
                    triggerOpenMergeRequestOnPush,
                    skipWorkInProgressMergeRequest));
        } else {
            return new NopPushSystemHookTriggerHandler();
        }
    }

    private static List<PushSystemHookTriggerHandler> retrieveHandlers(
            boolean triggerOnPush,
            boolean triggerToBranchDeleteRequest,
            TriggerOpenMergeRequest triggerOpenMergeRequestOnPush,
            boolean skipWorkInProgressMergeRequest) {
        List<PushSystemHookTriggerHandler> result = new ArrayList<>();
        if (triggerOnPush) {
            result.add(new PushSystemHookTriggerHandlerImpl(triggerToBranchDeleteRequest));
        }
        if (triggerOpenMergeRequestOnPush == TriggerOpenMergeRequest.both) {
            result.add(new OpenMergeRequestPushSystemHookTriggerHandler(skipWorkInProgressMergeRequest));
        }
        return result;
    }
}
