package com.dabsquared.gitlabjenkins.trigger.filter;

/**
 * @author Robin Müller
 */
public class MergeRequestLabelFilterFactory {

    private MergeRequestLabelFilterFactory() {}

    public static MergeRequestLabelFilter newMergeRequestLabelFilter(MergeRequestLabelFilterConfig config) {
        if (config == null) {
            return new NopMergeRequestLabelFilter();
        } else {
            return new MergeRequestLabelFilterImpl(config.getInclude(), config.getExclude());
        }
    }
}
