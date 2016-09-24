package com.dabsquared.gitlabjenkins.trigger.filter;

import com.google.common.base.Splitter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
        Set<String> result = new HashSet<>();
        for (String s : Splitter.on(',').omitEmptyStrings().trimResults().split(commaSeparatedString)) {
            result.add(s);
        }
        return result;
    }
}
