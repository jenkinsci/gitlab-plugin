package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.Action;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestObjectAttributes;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.google.common.base.Predicate;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class TriggerConfigChain implements Predicate<MergeRequestObjectAttributes> {
    private final List<Predicate<MergeRequestObjectAttributes>> chain = new ArrayList<>();

    public TriggerConfigChain addIf(boolean condition, Predicate<MergeRequestObjectAttributes> trigger) {
        if (condition) {
            this.chain.add(trigger);
        }
        return this;
    }

    public TriggerConfigChain addIf(boolean condition, EnumSet<State> states, EnumSet<Action> actions) {
        return addIf(condition, new StateAndActionConfig(states, actions));
    }

    public TriggerConfigChain addIf(boolean condition, Predicate<State> states, Predicate<Action> actions) {
        return addIf(condition, new StateAndActionConfig(states, actions));
    }

    @Override
    public boolean apply(@Nullable MergeRequestObjectAttributes mergeRequestObjectAttributes) {
        for (Predicate<MergeRequestObjectAttributes> predicate : chain) {
            if (predicate.apply(mergeRequestObjectAttributes)) {
                return true;
            }
        }
        return false;
    }
}
