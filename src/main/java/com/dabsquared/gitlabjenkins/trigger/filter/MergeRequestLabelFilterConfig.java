package com.dabsquared.gitlabjenkins.trigger.filter;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * @author Robin Müller
 */
public class MergeRequestLabelFilterConfig {

    private String include;
    private String exclude;

    /**
     * @deprecated use {@link #MergeRequestLabelFilterConfig()} with setters to configure an instance of this class.
     */
    @Deprecated
    public MergeRequestLabelFilterConfig(String include, String exclude) {
        this.include = include;
        this.exclude = exclude;
    }

    @DataBoundConstructor
    public MergeRequestLabelFilterConfig() { }

    public String getInclude() {
        return include;
    }

    public String getExclude() {
        return exclude;
    }

    @DataBoundSetter
    public void setInclude(String include) {
        this.include = include;
    }

    @DataBoundSetter
    public void setExclude(String exclude) {
        this.exclude = exclude;
    }
}
