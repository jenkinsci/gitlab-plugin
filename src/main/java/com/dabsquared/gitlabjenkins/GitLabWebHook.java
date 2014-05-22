package com.dabsquared.gitlabjenkins;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.RootAction;
import hudson.model.UnprotectedRootAction;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Daniel Brooks
 */
public class GitLabWebHook implements UnprotectedRootAction {

    public static final String WEBHOOK_URL = "gitlab-webhook";


    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return WEBHOOK_URL;
    }

    @RequirePOST
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

        GitLabPushRequest req = GitLabPushRequest.create(json);

        String repositoryUrl = req.getRepository().getUrl();
        if (repositoryUrl == null) {
            LOGGER.log(Level.WARNING, "No repository url found.");
            return;
        }

        Authentication old = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
        try {
            for (AbstractProject<?, ?> job : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
                GitLabPushTrigger trigger = job.getTrigger(GitLabPushTrigger.class);
                if (trigger == null) {
                    continue;
                }
                //if (RepositoryUrlCollector.collect(job).contains(repositoryUrl.toLowerCase())) {
                    trigger.onPost(req);
                //}
            }
        } finally {
            SecurityContextHolder.getContext().setAuthentication(old);
        }
    }


    @Extension
    public static class GitLabWebHookCrumbExclusion extends CrumbExclusion {

        @Override
        public boolean process(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {
            String pathInfo = req.getPathInfo();
            LOGGER.log(Level.FINE, "path: {0}", pathInfo);

            if (pathInfo != null && pathInfo.equals(getExclusionPath())) {
                chain.doFilter(req, resp);
                return true;
            }
            return false;
        }

        private String getExclusionPath() {
            return '/' + WEBHOOK_URL + '/';
        }
    }


    private static final Logger LOGGER = Logger.getLogger(GitLabWebHook.class.getName());

}
