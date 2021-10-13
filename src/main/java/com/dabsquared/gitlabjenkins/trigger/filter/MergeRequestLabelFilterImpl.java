package com.dabsquared.gitlabjenkins.trigger.filter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Robin Müller
 */
class MergeRequestLabelFilterImpl implements MergeRequestLabelFilter {

    private final Set<String> includeLabels;
    private final Set<String> excludeLabels;

    public MergeRequestLabelFilterImpl(String includeLabels, String excludeLabels) {
        this.includeLabels = convert(includeLabels);
        this.excludeLabels = convert(excludeLabels);
    }

    @Override
    public boolean isMergeRequestAllowed(Collection<String> labels) {
        return containsNoExcludeLabel(labels) && containsIncludeLabel(labels);
    }

    private boolean containsNoExcludeLabel(Collection<String> labels) {
        for (String excludeLabel : excludeLabels) {
            if (labels != null && labels.contains(excludeLabel)) {
                return false;
            }
        }
        return true;
    }

    private boolean containsIncludeLabel(Collection<String> labels) {
        for (String includeLabel : includeLabels) {
            if (labels != null && labels.contains(includeLabel)) {
                return true;
            }
        }
        return includeLabels.isEmpty();
    }

    private Set<String> convert(String commaSeparatedString) {
        return Arrays.stream(commaSeparatedString.split(",")).filter(s -> !s.isEmpty()).map(String::trim).collect(Collectors.toSet());
    }
}
