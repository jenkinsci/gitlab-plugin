package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Robin Müller
 */
public final class PushHookTriggerHandlerFactory {

    private PushHookTriggerHandlerFactory() {}

    public static PushHookTriggerHandler newPushHookTriggerHandler(
            boolean triggerOnPush,
            boolean triggerToBranchDeleteRequest,
            TriggerOpenMergeRequest triggerOpenMergeRequestOnPush,
            boolean skipWorkInProgressMergeRequest) {
        if (triggerOnPush || triggerOpenMergeRequestOnPush == TriggerOpenMergeRequest.both) {
            return new PushHookTriggerHandlerList(retrieveHandlers(
                    triggerOnPush,
                    triggerToBranchDeleteRequest,
                    triggerOpenMergeRequestOnPush,
                    skipWorkInProgressMergeRequest));
        } else {
            return new NopPushHookTriggerHandler();
        }
    }

    private static List<PushHookTriggerHandler> retrieveHandlers(
            boolean triggerOnPush,
            boolean triggerToBranchDeleteRequest,
            TriggerOpenMergeRequest triggerOpenMergeRequestOnPush,
            boolean skipWorkInProgressMergeRequest) {
        List<PushHookTriggerHandler> result = new ArrayList<>();
        if (triggerOnPush) {
            result.add(new PushHookTriggerHandlerImpl(triggerToBranchDeleteRequest));
        }
        if (triggerOpenMergeRequestOnPush == TriggerOpenMergeRequest.both) {
            result.add(new OpenMergeRequestPushHookTriggerHandler(skipWorkInProgressMergeRequest));
        }
        return result;
    }
}
