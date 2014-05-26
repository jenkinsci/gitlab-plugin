package com.dabsquared.gitlabjenkins;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.*;
import hudson.security.ACL;
import hudson.security.csrf.CrumbExclusion;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Daniel Brooks
 */

public class GitLabWebHook implements UnprotectedRootAction {

    public static final String WEBHOOK_URL = "projects";

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return WEBHOOK_URL;
    }

    //@RequirePOST For some reason the RequirePost is not working right.
    public void doIndex(StaplerRequest req) {
        LOGGER.log(Level.FINE, "WebHook called.");

        String payload = req.getParameter("payload");
        if (payload == null) {
            throw new IllegalArgumentException(
                    "Not intended to be browsed interactively (must specify payload parameter)");
        }

        //processPayload(payload);
    }


    private void processPayload(String payload) {
        JSONObject json = JSONObject.fromObject(payload);
        LOGGER.log(Level.FINE, "payload: {0}", json.toString(4));

        //Eventually parse the request here.
//        GitLabPushRequest req = GitLabPushRequest.create(json);
//
//        String repositoryUrl = req.getRepository().getUrl();
//        if (repositoryUrl == null) {
//            LOGGER.log(Level.WARNING, "No repository url found.");
//            return;
//        }

        Authentication old = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
        try {
            for (AbstractProject<?, ?> job : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
                GitLabPushTrigger trigger = job.getTrigger(GitLabPushTrigger.class);
                if (trigger == null) {
                    //This job does not have the Gitlab Trigger Enabled so skip it.
                    continue;
                }

                //Here we trigger the ush
                //trigger.onPost(req);

            }
        } finally {
            SecurityContextHolder.getContext().setAuthentication(old);
        }
    }


    private static final Logger LOGGER = Logger.getLogger(GitLabWebHook.class.getName());

}
