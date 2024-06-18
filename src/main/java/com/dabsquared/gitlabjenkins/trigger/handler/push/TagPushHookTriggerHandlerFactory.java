package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;
import java.util.ArrayList;
import java.util.List;

public final class TagPushHookTriggerHandlerFactory {

    private TagPushHookTriggerHandlerFactory() {}

    public static TagPushHookTriggerHandler newPushHookTriggerHandler(
            boolean triggerOnPush,
            boolean triggerToBranchDeleteRequest,
            TriggerOpenMergeRequest triggerOpenMergeRequestOnPush,
            boolean skipWorkInProgressMergeRequest) {
        if (triggerOnPush || triggerOpenMergeRequestOnPush == TriggerOpenMergeRequest.both) {
            return new TagPushHookTriggerHandlerList(retrieveHandlers(
                    triggerOnPush,
                    triggerToBranchDeleteRequest,
                    triggerOpenMergeRequestOnPush,
                    skipWorkInProgressMergeRequest));
        } else {
            return new NopTagPushHookTriggerHandler();
        }
    }

    private static List<TagPushHookTriggerHandler> retrieveHandlers(
            boolean triggerOnPush,
            boolean triggerToBranchDeleteRequest,
            TriggerOpenMergeRequest triggerOpenMergeRequestOnPush,
            boolean skipWorkInProgressMergeRequest) {
        List<TagPushHookTriggerHandler> result = new ArrayList<>();
        if (triggerOnPush) {
            result.add(new TagPushHookTriggerHandlerImpl(triggerToBranchDeleteRequest));
        }
        if (triggerOpenMergeRequestOnPush == TriggerOpenMergeRequest.both) {
            result.add(new OpenMergeRequestTagPushHookTriggerHandler(skipWorkInProgressMergeRequest));
        }
        return result;
    }
}
