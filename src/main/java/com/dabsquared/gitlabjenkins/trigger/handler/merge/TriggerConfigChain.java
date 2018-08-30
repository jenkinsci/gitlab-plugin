package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.Action;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestObjectAttributes;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.google.common.base.Predicate;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public class TriggerConfigChain implements Predicate<MergeRequestObjectAttributes> {
    private final List<Predicate<MergeRequestObjectAttributes>> acceptRules = new ArrayList<>();
    private final List<Predicate<MergeRequestObjectAttributes>> rejectRules = new ArrayList<>();


    public TriggerConfigChain rejectUnless(boolean condition, Predicate<MergeRequestObjectAttributes> trigger) {
        if (!condition) {
            this.rejectRules.add(trigger);
        }
        return this;
    }

    public TriggerConfigChain rejectUnless(boolean condition, EnumSet<State> states, EnumSet<Action> actions) {
        return rejectUnless(condition, new StateAndActionConfig(states, actions));
    }

    public TriggerConfigChain acceptOnlyIf(boolean condition, EnumSet<State> states, EnumSet<Action> actions) {
        return rejectUnless(condition, states, actions)
            .acceptIf(condition, states, actions);
    }

    public TriggerConfigChain acceptIf(boolean condition, Predicate<MergeRequestObjectAttributes> trigger) {
        if (condition) {
            this.acceptRules.add(trigger);
        }
        return this;
    }

    public TriggerConfigChain acceptIf(boolean condition, EnumSet<State> states, EnumSet<Action> actions) {
        return acceptIf(condition, new StateAndActionConfig(states, actions));
    }

    public TriggerConfigChain add(Collection<State> states, Collection<Action> actions) {
        return acceptIf(true, new StateAndActionConfig(states, actions));
    }

    public TriggerConfigChain acceptIf(boolean condition, Predicate<State> states, Predicate<Action> actions) {
        return acceptIf(condition, new StateAndActionConfig(states, actions));
    }

    @Override
    public boolean apply(@Nullable MergeRequestObjectAttributes mergeRequestObjectAttributes) {
        for (Predicate<MergeRequestObjectAttributes> predicate : rejectRules) {
            if (predicate.apply(mergeRequestObjectAttributes)) {
                return false;
            }
        }

        for (Predicate<MergeRequestObjectAttributes> predicate : acceptRules) {
            if (predicate.apply(mergeRequestObjectAttributes)) {
                return true;
            }
        }
        return false;
    }
}
