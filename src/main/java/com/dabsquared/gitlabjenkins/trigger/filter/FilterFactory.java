package com.dabsquared.gitlabjenkins.trigger.filter;

import org.apache.commons.lang.StringUtils;

/**
 * @author Robin Müller
 * @author Roland Hauser
 */
public final class FilterFactory {

    private FilterFactory() { }

    public static Filter newFilesFilter(String includeFilesRegex) {
        if (StringUtils.isEmpty(includeFilesRegex)) {
            return new AcceptAllFilter();
        }
        return new RegexBasedFilter(includeFilesRegex);
    }

    public static Filter newBranchFilter(BranchFilterConfig config) {
        switch (config.getType()) {
            case NameBasedFilter:
                return new NameBasedFilter(config.getIncludeBranchesSpec(), config.getExcludeBranchesSpec());
            case RegexBasedFilter:
                return new RegexBasedFilter(config.getTargetBranchRegex());
            default:
                return new AcceptAllFilter();
        }
    }
}
