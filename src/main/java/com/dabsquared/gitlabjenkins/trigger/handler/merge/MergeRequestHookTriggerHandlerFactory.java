package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import static java.util.EnumSet.of;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.StringUtils.split;
import static org.apache.commons.lang.StringUtils.trimToEmpty;

import com.dabsquared.gitlabjenkins.MergeRequestTriggerConfig;
import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;
import java.util.Set;
import java.util.stream.Stream;
import org.gitlab4j.api.Constants.ActionType;
import org.gitlab4j.api.Constants.MergeRequestState;

/**
 * @author Robin Müller
 */
public final class MergeRequestHookTriggerHandlerFactory {

    private MergeRequestHookTriggerHandlerFactory() {}

    public static MergeRequestHookTriggerHandler newMergeRequestHookTriggerHandler(
            boolean triggerOnMergeRequest,
            boolean triggerOnlyWithNewCommitsPushed,
            boolean triggerOnAcceptedMergeRequest,
            boolean triggerOnClosedMergeRequest,
            TriggerOpenMergeRequest triggerOpenMergeRequest,
            boolean skipWorkInProgressMergeRequest,
            String labelsThatForcesBuildIfAdded,
            boolean triggerOnApprovedMergeRequest,
            boolean cancelPendingBuildsOnUpdate) {

        TriggerConfigChain chain = new TriggerConfigChain();
        chain.acceptOnlyIf(
                        triggerOpenMergeRequest != TriggerOpenMergeRequest.never,
                        of(MergeRequestState.ALL), 
                        of(ActionType.UPDATED))
                .acceptOnlyIf(triggerOnApprovedMergeRequest, null, of(ActionType.APPROVED))
                .acceptIf(triggerOnMergeRequest, of(MergeRequestState.OPENED), null)
                .acceptIf(triggerOnAcceptedMergeRequest, null, of(ActionType.MERGED))
                .acceptIf(triggerOnClosedMergeRequest, null, of(ActionType.CLOSED))
                .acceptIf(triggerOnClosedMergeRequest, of(MergeRequestState.CLOSED), null);

        Set<String> labelsThatForcesBuildIfAddedSet =
                Stream.of(split(trimToEmpty(labelsThatForcesBuildIfAdded), ",")).collect(toSet());
        return new MergeRequestHookTriggerHandlerImpl(
                chain,
                triggerOnlyWithNewCommitsPushed,
                skipWorkInProgressMergeRequest,
                labelsThatForcesBuildIfAddedSet,
                cancelPendingBuildsOnUpdate);
    }

    public static MergeRequestHookTriggerHandler newMergeRequestHookTriggerHandler(MergeRequestTriggerConfig config) {
        return newMergeRequestHookTriggerHandler(
                config.getTriggerOnMergeRequest(),
                config.isTriggerOnlyIfNewCommitsPushed(),
                config.isTriggerOnAcceptedMergeRequest(),
                config.isTriggerOnClosedMergeRequest(),
                config.getTriggerOpenMergeRequestOnPush(),
                config.isSkipWorkInProgressMergeRequest(),
                config.getLabelsThatForcesBuildIfAdded(),
                config.isTriggerOnApprovedMergeRequest(),
                config.getCancelPendingBuildsOnUpdate());
    }

    public static Config withConfig() {
        return new Config();
    }

    public static class Config implements MergeRequestTriggerConfig {
        private boolean triggerOnMergeRequest = true;
        private boolean triggerOnlyIfNewCommitsPushed = false;
        private boolean triggerOnAcceptedMergeRequest = false;
        private boolean triggerOnClosedMergeRequest = false;
        private TriggerOpenMergeRequest triggerOpenMergeRequest = TriggerOpenMergeRequest.never;
        private boolean skipWorkInProgressMergeRequest = false;
        private String labelsThatForcesBuildIfAdded;
        private boolean triggerOnApprovedMergeRequest = false;
        private boolean cancelPendingBuildsOnUpdate = false;

        @Override
        public boolean getTriggerOnMergeRequest() {
            return triggerOnMergeRequest;
        }

        @Override
        public boolean isTriggerOnlyIfNewCommitsPushed() {
            return triggerOnlyIfNewCommitsPushed;
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
        public String getLabelsThatForcesBuildIfAdded() {
            return labelsThatForcesBuildIfAdded;
        }

        @Override
        public boolean getCancelPendingBuildsOnUpdate() {
            return cancelPendingBuildsOnUpdate;
        }

        public Config setTriggerOnMergeRequest(boolean triggerOnMergeRequest) {
            this.triggerOnMergeRequest = triggerOnMergeRequest;
            return this;
        }

        public Config setTriggerOnlyIfNewCommitsPushed(boolean triggerOnlyIfNewCommitsPushed) {
            this.triggerOnlyIfNewCommitsPushed = triggerOnlyIfNewCommitsPushed;
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

        public Config setLabelsThatForcesBuildIfAdded(String labelsThatForcesBuildIfAdded) {
            this.labelsThatForcesBuildIfAdded = labelsThatForcesBuildIfAdded;
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
