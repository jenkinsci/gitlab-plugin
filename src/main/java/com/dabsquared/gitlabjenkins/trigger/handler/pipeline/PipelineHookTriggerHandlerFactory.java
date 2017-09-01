package com.dabsquared.gitlabjenkins.trigger.handler.pipeline;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Milena Zachow
 */
public final class PipelineHookTriggerHandlerFactory {

    public static final String SUCCESS = "success";

    private PipelineHookTriggerHandlerFactory() {
    }

    public static PipelineHookTriggerHandler newPipelineHookTriggerHandler(boolean triggerOnPipelineEvent) {
        if (triggerOnPipelineEvent) {
            return new PipelineHookTriggerHandlerImpl(retrieve(triggerOnPipelineEvent));
        } else {
            return new NopPipelineHookTriggerHandler();
        }
    }


    private static List<String> retrieve(boolean triggerOnPipelineEvent) {
        List<String> result = new ArrayList<>();
        if (triggerOnPipelineEvent) {
            result.add(SUCCESS);
        }
        return result;
    }
}
