package com.dabsquared.gitlabjenkins.webhook;

import hudson.Extension;
import hudson.model.UnprotectedRootAction;
import hudson.security.csrf.CrumbExclusion;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

/**
 * @author Daniel Brooks
 */
@Extension
public class GitLabWebHook implements UnprotectedRootAction {

    public static final String WEBHOOK_URL = "project";

    private static final Logger LOGGER = Logger.getLogger(GitLabWebHook.class.getName());

    private final transient ActionResolver actionResolver = new ActionResolver();

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return WEBHOOK_URL;
    }

    public void getDynamic(final String projectName, final StaplerRequest2 request, StaplerResponse2 response) {
        LOGGER.log(Level.INFO, "WebHook called with url: {0}", request.getRequestURIWithQueryString());
        actionResolver.resolve(projectName, request).execute(response);
    }

    @Extension
    public static class GitlabWebHookCrumbExclusion extends CrumbExclusion {
        @Override
        public boolean process(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
                throws IOException, ServletException {
            String pathInfo = req.getPathInfo();
            if (pathInfo != null && pathInfo.startsWith('/' + WEBHOOK_URL + '/')) {
                chain.doFilter(req, resp);
                return true;
            }
            return false;
        }
    }
}
