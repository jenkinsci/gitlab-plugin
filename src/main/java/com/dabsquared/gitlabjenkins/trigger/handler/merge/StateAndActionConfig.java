package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;
import org.gitlab4j.api.Constants.ActionType;
import org.gitlab4j.api.Constants.MergeRequestState;
import org.gitlab4j.api.webhook.MergeRequestEvent.ObjectAttributes;

class StateAndActionConfig implements Predicate<ObjectAttributes> {
    private final Predicate<MergeRequestState> states;
    private final Predicate<ActionType> actions;

    public StateAndActionConfig(Collection<MergeRequestState> allowedStates, Collection<ActionType> allowedActions) {
        this(nullOrContains(allowedStates), nullOrContains(allowedActions));
    }

    public StateAndActionConfig(Predicate<MergeRequestState> states, Predicate<ActionType> actions) {
        this.states = states == null ? unused -> true : states;
        this.actions = actions == null ? unused -> true : actions;
    }

    @Override
    public boolean test(ObjectAttributes mergeRequestObjectAttributes) {
        return states.test(MergeRequestState.valueOf(mergeRequestObjectAttributes.getState().toUpperCase()))
                && actions.test(ActionType.valueOf(mergeRequestObjectAttributes.getAction().toUpperCase()));
    }

    static <T> Predicate<T> nullOrContains(final Collection<T> collection) {
        return collection == null ? unused -> true : (t -> t == null || collection.contains(t));
    }

    static <T> Predicate<T> notEqual(final T value) {
        return t -> !Objects.equals(t, value);
    }
}
