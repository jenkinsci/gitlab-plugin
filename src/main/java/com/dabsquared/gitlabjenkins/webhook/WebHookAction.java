package com.dabsquared.gitlabjenkins.webhook;

import org.kohsuke.stapler.StaplerResponse;

/**
 * @author Robin Müller
 */
public interface WebHookAction {
    void execute(StaplerResponse response);
    void executeNoResponse(StaplerResponse response);
}
