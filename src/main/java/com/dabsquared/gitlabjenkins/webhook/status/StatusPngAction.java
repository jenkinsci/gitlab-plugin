package com.dabsquared.gitlabjenkins.webhook.status;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.StaplerResponse;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 * @author Robin Müller
 */
class StatusPngAction extends BuildStatusAction {
    protected StatusPngAction(AbstractProject<?, ?> project, AbstractBuild<?, ?> build) {
        super(project, build);
    }

    @Override
    protected void writeStatusBody(StaplerResponse response, AbstractBuild<?, ?> build, BuildStatus status) {
        try {
            response.setHeader("Expires","Fri, 01 Jan 1984 00:00:00 GMT");
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
