package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;

/**
 * @author Robin Müller
 */
public interface PushHookTriggerConfig {

    boolean getCiSkip();

    BranchFilter getBranchFilter();
}
