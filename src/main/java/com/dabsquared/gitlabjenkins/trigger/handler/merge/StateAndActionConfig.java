package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.Action;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestObjectAttributes;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Objects;

class StateAndActionConfig implements Predicate<MergeRequestObjectAttributes> {
    private final Predicate<State> states;
    private final Predicate<Action> actions;

    public StateAndActionConfig(Collection<State> allowedStates, Collection<Action> allowedActions) {
        this(nullOrContains(allowedStates), nullOrContains(allowedActions));
    }

    public StateAndActionConfig(Predicate<State> states, Predicate<Action> actions) {
        this.states = states == null ? Predicates.<State>alwaysTrue() : states;
        this.actions = actions == null ? Predicates.<Action>alwaysTrue() : actions;
    }

    @Override
    public boolean apply(MergeRequestObjectAttributes mergeRequestObjectAttributes) {
        return
            states.apply(mergeRequestObjectAttributes.getState()) &&
            actions.apply(mergeRequestObjectAttributes.getAction());
    }

    static <T> Predicate<T> nullOrContains(final Collection<T> collection) {
        return collection == null ? Predicates.<T>alwaysTrue() : new Predicate<T>() {
            @Override
            public boolean apply(@Nullable T t) {
                return t == null || collection.contains(t);
            }
        };
    }

    static <T> Predicate<T> notEqual(final T value) {
        return new Predicate<T>() {
            @Override
            public boolean apply(@Nullable T t) {
                return !Objects.equals(t, value);
            }
        };
    }
}
