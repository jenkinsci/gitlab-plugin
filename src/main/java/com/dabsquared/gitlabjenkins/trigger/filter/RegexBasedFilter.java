package com.dabsquared.gitlabjenkins.trigger.filter;

import org.apache.commons.lang.StringUtils;

/**
 * @author Robin Müller
 */
class RegexBasedFilter implements BranchFilter {

    private final String sourceRegex;
    private final String targetRegex;

    public RegexBasedFilter(String sourceRegex, String targetRegex) {
        this.sourceRegex = sourceRegex;
        this.targetRegex = targetRegex;
    }

    @Override
    public boolean isBranchAllowed(String sourceBranchName, String targetBranchName) {
        if (StringUtils.isEmpty(targetBranchName))
            return StringUtils.isEmpty(sourceRegex) || sourceBranchName.matches(sourceRegex);
        else
            return ( StringUtils.isEmpty(sourceRegex) || sourceBranchName.matches(sourceRegex) )
                  && ( StringUtils.isEmpty(targetRegex) || targetBranchName.matches(targetRegex) );
    }
}
