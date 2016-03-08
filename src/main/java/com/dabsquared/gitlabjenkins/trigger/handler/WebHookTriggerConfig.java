package com.dabsquared.gitlabjenkins.trigger.handler;

import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;

/**
 * @author Robin Müller
 */
public interface WebHookTriggerConfig {

    boolean getCiSkip();

    BranchFilter getBranchFilter();
}
