package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.Action;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestObjectAttributes;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

class StateAndActionConfig implements Predicate<MergeRequestObjectAttributes> {
    private final Predicate<State> states;
    private final Predicate<Action> actions;

    public StateAndActionConfig(Collection<State> allowedStates, Collection<Action> allowedActions) {
        this(nullOrContains(allowedStates), nullOrContains(allowedActions));
    }

    public StateAndActionConfig(Predicate<State> states, Predicate<Action> actions) {
        this.states = states == null ? unused -> true : states;
        this.actions = actions == null ? unused -> true : actions;
    }

    @Override
    public boolean test(MergeRequestObjectAttributes mergeRequestObjectAttributes) {
        return
            states.test(mergeRequestObjectAttributes.getState()) &&
            actions.test(mergeRequestObjectAttributes.getAction());
    }

    static <T> Predicate<T> nullOrContains(final Collection<T> collection) {
        return collection == null ? unused -> true : (t -> t == null || collection.contains(t));
    }

    static <T> Predicate<T> notEqual(final T value) {
        return t -> !Objects.equals(t, value);
    }
}
