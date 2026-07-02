package com.dabsquared.gitlabjenkins.trigger.handler.pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Milena Zachow
 */
public final class PipelineHookTriggerHandlerFactory {

    public static final String SUCCESS = "success";
    public static final String FAILED = "failed";

    private PipelineHookTriggerHandlerFactory() {}

    public static PipelineHookTriggerHandler newPipelineHookTriggerHandler(
            boolean triggerOnPipelineEvent, boolean triggerOnFailedPipeline) {
        if (triggerOnPipelineEvent || triggerOnFailedPipeline) {
            return new PipelineHookTriggerHandlerImpl(retrieve(triggerOnPipelineEvent, triggerOnFailedPipeline));
        } else {
            return new NopPipelineHookTriggerHandler();
        }
    }

    public static PipelineHookTriggerHandler newPipelineHookTriggerHandler(boolean triggerOnPipelineEvent) {
        return newPipelineHookTriggerHandler(triggerOnPipelineEvent, false);
    }

    private static List<String> retrieve(boolean triggerOnPipelineEvent, boolean triggerOnFailedPipeline) {
        List<String> result = new ArrayList<>();
        if (triggerOnPipelineEvent) {
            result.add(SUCCESS);
        }
        if (triggerOnFailedPipeline) {
            result.add(FAILED);
        }
        return result;
    }
}
