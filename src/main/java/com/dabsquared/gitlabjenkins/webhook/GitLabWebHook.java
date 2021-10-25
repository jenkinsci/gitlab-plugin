package com.dabsquared.gitlabjenkins.webhook;

import hudson.Extension;
import hudson.model.UnprotectedRootAction;
import hudson.security.csrf.CrumbExclusion;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Daniel Brooks
 */

@Extension
public class GitLabWebHook implements UnprotectedRootAction {

    public static final String WEBHOOK_URL = "project";

    private static final Logger LOGGER = Logger.getLogger(GitLabWebHook.class.getName());

    private transient final ActionResolver actionResolver = new ActionResolver();

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return WEBHOOK_URL;
    }

    public void getDynamic(final String projectName, final StaplerRequest request, StaplerResponse response) {
        LOGGER.log(Level.INFO, "WebHook called with url: {0}", request.getRequestURIWithQueryString());
        actionResolver.resolve(projectName, request).execute(response);
    }

    @Extension
    public static class GitlabWebHookCrumbExclusion extends CrumbExclusion {
        @Override
        public boolean process(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {
            String pathInfo = req.getPathInfo();
            if (pathInfo != null && pathInfo.startsWith('/' + WEBHOOK_URL + '/')) {
                chain.doFilter(req, resp);
                return true;
            }
            return false;
        }
    }
}
