package com.dabsquared.gitlabjenkins.trigger.filter;

import com.dabsquared.gitlabjenkins.gitlab.api.model.Label;
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
    public boolean isMergeRequestAllowed(Collection<Label> labels) {
        return containsNoExcludeLabel(labels) && containsIncludeLabel(labels);
    }

    private boolean containsNoExcludeLabel(Collection<Label> labels) {
        for (Label label : labels) {
            for(String excludeLabel : excludeLabels)
                if (label != null && label.getTitle() != null && label.getTitle().contains(excludeLabel)) {
                    return false;
                }
        }
        return true;
    }

    private boolean containsIncludeLabel(Collection<Label> labels) {
        for (Label label : labels) {
            for (String includeLabel : includeLabels) {
                if (label != null && label.getTitle() != null && label.getTitle().contains(includeLabel)) {
                    return true;
                }
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
