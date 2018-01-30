package com.dabsquared.gitlabjenkins.trigger.filter;

import com.dabsquared.gitlabjenkins.gitlab.api.model.Label;

import java.util.Collection;

/**
 * @author Robin Müller
 */
public interface MergeRequestLabelFilter {
    boolean isMergeRequestAllowed(Collection<Label> labels);
}
