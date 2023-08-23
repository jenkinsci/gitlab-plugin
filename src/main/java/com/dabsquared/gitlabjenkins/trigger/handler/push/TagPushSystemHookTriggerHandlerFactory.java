package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;
import java.util.ArrayList;
import java.util.List;

public final class TagPushSystemHookTriggerHandlerFactory {

    private TagPushSystemHookTriggerHandlerFactory() {}

    public static TagPushSystemHookTriggerHandler newPushHookTriggerHandler(
            boolean triggerOnPush,
            boolean triggerToBranchDeleteRequest,
            TriggerOpenMergeRequest triggerOpenMergeRequestOnPush,
            boolean skipWorkInProgressMergeRequest) {
        if (triggerOnPush || triggerOpenMergeRequestOnPush == TriggerOpenMergeRequest.both) {
            return new TagPushSystemHookTriggerHandlerList(retrieveHandlers(
                    triggerOnPush,
                    triggerToBranchDeleteRequest,
                    triggerOpenMergeRequestOnPush,
                    skipWorkInProgressMergeRequest));
        } else {
            return new NopTagPushSystemHookTriggerHandler();
        }
    }

    private static List<TagPushSystemHookTriggerHandler> retrieveHandlers(
            boolean triggerOnPush,
            boolean triggerToBranchDeleteRequest,
            TriggerOpenMergeRequest triggerOpenMergeRequestOnPush,
            boolean skipWorkInProgressMergeRequest) {
        List<TagPushSystemHookTriggerHandler> result = new ArrayList<>();
        if (triggerOnPush) {
            result.add(new TagPushSystemHookTriggerHandlerImpl(triggerToBranchDeleteRequest));
        }
        if (triggerOpenMergeRequestOnPush == TriggerOpenMergeRequest.both) {
            result.add(new OpenMergeRequestTagPushSystemHookTriggerHandler(skipWorkInProgressMergeRequest));
        }
        return result;
    }
}
