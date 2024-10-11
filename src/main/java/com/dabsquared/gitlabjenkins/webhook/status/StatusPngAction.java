package com.dabsquared.gitlabjenkins.webhook.status;

import hudson.model.Job;
import hudson.model.Run;
import hudson.util.HttpResponses;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.StaplerResponse2;

/**
 * @author Robin Müller
 */
class StatusPngAction extends BuildStatusAction {
    protected StatusPngAction(Job<?, ?> project, Run<?, ?> build) {
        super(project, build);
    }

    @Override
    protected void writeStatusBody(StaplerResponse2 response, Run<?, ?> build, BuildStatus status) {
        try {
            response.setHeader("Expires", "Fri, 01 Jan 1984 00:00:00 GMT");
            response.setHeader("Cache-Control", "no-cache, private");
            response.setHeader("Content-Type", "image/png");
            IOUtils.copy(getStatusImage(status), response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            throw HttpResponses.error(500, "Could not generate response.");
        }
    }

    private InputStream getStatusImage(BuildStatus status) {
        switch (status) {
            case RUNNING:
                return getClass().getResourceAsStream("running.png");
            case SUCCESS:
                return getClass().getResourceAsStream("success.png");
            case FAILED:
                return getClass().getResourceAsStream("failed.png");
            case UNSTABLE:
                return getClass().getResourceAsStream("unstable.png");
            default:
                return getClass().getResourceAsStream("unknown.png");
        }
    }
}
