package com.dabsquared.gitlabjenkins.trigger.filter;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Robin Müller
 */
public class MergeRequestLabelFilterConfig {

    private final String include;
    private final String exclude;

    @DataBoundConstructor
    public MergeRequestLabelFilterConfig(String include, String exclude) {
        this.include = include;
        this.exclude = exclude;
    }

    public String getInclude() {
        return include;
    }

    public String getExclude() {
        return exclude;
    }
}
