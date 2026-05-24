package com.dabsquared.gitlabjenkins.webhook;

import org.kohsuke.stapler.StaplerResponse2;

/**
 * @author Robin Müller
 */
public interface WebHookAction {
    void execute(StaplerResponse2 response);
}
