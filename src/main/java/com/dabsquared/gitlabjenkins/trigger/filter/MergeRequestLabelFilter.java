package com.dabsquared.gitlabjenkins.trigger.filter;

import java.util.Collection;

/**
 * @author Robin Müller
 */
public interface MergeRequestLabelFilter {
    boolean isMergeRequestAllowed(Collection<String> labels);
}
