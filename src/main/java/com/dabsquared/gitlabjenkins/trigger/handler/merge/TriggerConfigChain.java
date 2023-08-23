package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import org.gitlab4j.api.Constants.ActionType;
import org.gitlab4j.api.Constants.MergeRequestState;
import org.gitlab4j.api.webhook.MergeRequestEvent.ObjectAttributes;

public class TriggerConfigChain implements Predicate<ObjectAttributes> {
    private final List<Predicate<ObjectAttributes>> acceptRules = new ArrayList<>();
    private final List<Predicate<ObjectAttributes>> rejectRules = new ArrayList<>();

    public TriggerConfigChain rejectUnless(boolean condition, Predicate<ObjectAttributes> trigger) {
        if (!condition) {
            this.rejectRules.add(trigger);
        }
        return this;
    }

    public TriggerConfigChain rejectUnless(
            boolean condition, EnumSet<MergeRequestState> states, EnumSet<ActionType> actions) {
        return rejectUnless(condition, new StateAndActionConfig(states, actions));
    }

    public TriggerConfigChain acceptOnlyIf(
            boolean condition, EnumSet<MergeRequestState> states, EnumSet<ActionType> actions) {
        return rejectUnless(condition, states, actions).acceptIf(condition, states, actions);
    }

    public TriggerConfigChain acceptIf(boolean condition, Predicate<ObjectAttributes> trigger) {
        if (condition) {
            this.acceptRules.add(trigger);
        }
        return this;
    }

    public TriggerConfigChain acceptIf(
            boolean condition, EnumSet<MergeRequestState> states, EnumSet<ActionType> actions) {
        return acceptIf(condition, new StateAndActionConfig(states, actions));
    }

    public TriggerConfigChain add(Collection<MergeRequestState> states, Collection<ActionType> actions) {
        return acceptIf(true, new StateAndActionConfig(states, actions));
    }

    public TriggerConfigChain acceptIf(
            boolean condition, Predicate<MergeRequestState> states, Predicate<ActionType> actions) {
        return acceptIf(condition, new StateAndActionConfig(states, actions));
    }

    @Override
    public boolean test(ObjectAttributes mergeRequestObjectAttributes) {
        for (Predicate<ObjectAttributes> predicate : rejectRules) {
            if (predicate.test(mergeRequestObjectAttributes)) {
                return false;
            }
        }

        for (Predicate<ObjectAttributes> predicate : acceptRules) {
            if (predicate.test(mergeRequestObjectAttributes)) {
                return true;
            }
        }
        return false;
    }
}
