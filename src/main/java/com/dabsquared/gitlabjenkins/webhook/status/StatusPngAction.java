package com.dabsquared.gitlabjenkins.webhook.status;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.kohsuke.stapler.StaplerResponse;

import java.io.File;
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
        Authentication old = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
        try {
            URL resourceUrl = new URL(Jenkins.getInstance().getPlugin("gitlab-plugin").getWrapper().baseResourceURL + getStatusImageUrl(status));
            response.setHeader("Expires","Fri, 01 Jan 1984 00:00:00 GMT");
            response.setHeader("Cache-Control", "no-cache, private");
            response.setHeader("Content-Type", "image/png");
            hudson.util.IOUtils.copy(new File(resourceUrl.toURI()), response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            throw HttpResponses.error(500, "Could not generate response.");
        } finally {
            SecurityContextHolder.getContext().setAuthentication(old);
        }
    }

    private String getStatusImageUrl(BuildStatus status) {
        if(status == BuildStatus.RUNNING) {
            return "images/running.png";
        } else if (status == BuildStatus.SUCCESS) {
            return "images/success.png";
        } else if (status == BuildStatus.FAILED) {
            return "images/failed.png";
        } else if (status == BuildStatus.UNSTABLE) {
            return "images/unstable.png";
        } else {
            return "images/unknown.png";
        }
    }
}
