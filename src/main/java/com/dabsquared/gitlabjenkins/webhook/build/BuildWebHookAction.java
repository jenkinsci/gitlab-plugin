package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.webhook.WebHookAction;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author Xinran Xiao
 */
abstract class BuildWebHookAction implements WebHookAction {
    abstract void processForCompatibility();
    abstract void execute();

    public final void execute(StaplerResponse response) {
        processForCompatibility();
        execute();
    }
}
