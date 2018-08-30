package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.MergeRequestTriggerConfig;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.Action;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;

import static com.dabsquared.gitlabjenkins.trigger.handler.merge.StateAndActionConfig.notEqual;
import static com.dabsquared.gitlabjenkins.trigger.handler.merge.StateAndActionConfig.nullOrContains;
import static java.util.EnumSet.of;

/**
 * @author Robin Müller
 */
public final class MergeRequestHookTriggerHandlerFactory {

    private MergeRequestHookTriggerHandlerFactory() {}

    public static MergeRequestHookTriggerHandler newMergeRequestHookTriggerHandler(boolean triggerOnMergeRequest,
    		                                                                       boolean triggerOnAcceptedMergeRequest,
    		                                                                       boolean triggerOnClosedMergeRequest,
                                                                                   TriggerOpenMergeRequest triggerOpenMergeRequest,
                                                                                   boolean skipWorkInProgressMergeRequest,
                                                                                   boolean triggerOnApprovedMergeRequest,
                                                                                   boolean cancelPendingBuildsOnUpdate) {

        TriggerConfigChain chain = new TriggerConfigChain();
        chain
            .acceptOnlyIf(triggerOnApprovedMergeRequest, null, of(Action.approved))
            .acceptIf(triggerOnMergeRequest, of(State.opened, State.reopened), null)
            .acceptIf(triggerOnAcceptedMergeRequest, null, of(Action.merge))
            .acceptIf(triggerOnClosedMergeRequest, null, of(Action.closed))
            .acceptIf(triggerOpenMergeRequest != TriggerOpenMergeRequest.never, of(State.updated), null)
        ;

        return new MergeRequestHookTriggerHandlerImpl(chain, skipWorkInProgressMergeRequest, cancelPendingBuildsOnUpdate);
    }

    public static MergeRequestHookTriggerHandler newMergeRequestHookTriggerHandler(MergeRequestTriggerConfig config) {
        return newMergeRequestHookTriggerHandler(config.getTriggerOnMergeRequest(),
            config.isTriggerOnAcceptedMergeRequest(),
            config.isTriggerOnClosedMergeRequest(),
            config.getTriggerOpenMergeRequestOnPush(),
            config.isSkipWorkInProgressMergeRequest(),
            config.isTriggerOnApprovedMergeRequest(),
            config.getCancelPendingBuildsOnUpdate());
    }

    public static Config withConfig() {
        return new Config();
    }

    public static class Config implements MergeRequestTriggerConfig {
        private boolean triggerOnMergeRequest = true;
        private boolean triggerOnAcceptedMergeRequest = false;
        private boolean triggerOnClosedMergeRequest = false;
        private TriggerOpenMergeRequest triggerOpenMergeRequest = TriggerOpenMergeRequest.never;
        private boolean skipWorkInProgressMergeRequest = false;
        private boolean triggerOnApprovedMergeRequest = false;
        private boolean cancelPendingBuildsOnUpdate = false;

        @Override
        public boolean getTriggerOnMergeRequest() {
            return triggerOnMergeRequest;
        }

        @Override
        public boolean isTriggerOnAcceptedMergeRequest() {
            return triggerOnAcceptedMergeRequest;
        }

        @Override
        public boolean isTriggerOnApprovedMergeRequest() {
            return triggerOnApprovedMergeRequest;
        }

        @Override
        public boolean isTriggerOnClosedMergeRequest() {
            return triggerOnClosedMergeRequest;
        }

        @Override
        public TriggerOpenMergeRequest getTriggerOpenMergeRequestOnPush() {
            return triggerOpenMergeRequest;
        }

        @Override
        public boolean isSkipWorkInProgressMergeRequest() {
            return skipWorkInProgressMergeRequest;
        }

        @Override
        public boolean getCancelPendingBuildsOnUpdate() {
            return cancelPendingBuildsOnUpdate;
        }

        public Config setTriggerOnMergeRequest(boolean triggerOnMergeRequest) {
            this.triggerOnMergeRequest = triggerOnMergeRequest;
            return this;
        }

        public Config setTriggerOnAcceptedMergeRequest(boolean triggerOnAcceptedMergeRequest) {
            this.triggerOnAcceptedMergeRequest = triggerOnAcceptedMergeRequest;
            return this;
        }

        public Config setTriggerOnClosedMergeRequest(boolean triggerOnClosedMergeRequest) {
            this.triggerOnClosedMergeRequest = triggerOnClosedMergeRequest;
            return this;
        }

        public Config setTriggerOpenMergeRequest(TriggerOpenMergeRequest triggerOpenMergeRequest) {
            this.triggerOpenMergeRequest = triggerOpenMergeRequest;
            return this;
        }

        public Config setSkipWorkInProgressMergeRequest(boolean skipWorkInProgressMergeRequest) {
            this.skipWorkInProgressMergeRequest = skipWorkInProgressMergeRequest;
            return this;
        }

        public Config setTriggerOnApprovedMergeRequest(boolean triggerOnApprovedMergeRequest) {
            this.triggerOnApprovedMergeRequest = triggerOnApprovedMergeRequest;
            return this;
        }

        public Config setCancelPendingBuildsOnUpdate(boolean cancelPendingBuildsOnUpdate) {
            this.cancelPendingBuildsOnUpdate = cancelPendingBuildsOnUpdate;
            return this;
        }

        public MergeRequestHookTriggerHandler build() {
            return newMergeRequestHookTriggerHandler(this);
        }
    }
}
