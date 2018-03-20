package com.dabsquared.gitlabjenkins.trigger.filter;

import com.dabsquared.gitlabjenkins.gitlab.api.model.Label;

import java.util.Collection;

/**
 * @author Robin Müller
 */
class NopMergeRequestLabelFilter implements MergeRequestLabelFilter {
    @Override
    public boolean isMergeRequestAllowed(Collection<Label> labels) {
        return true;
    }
}
