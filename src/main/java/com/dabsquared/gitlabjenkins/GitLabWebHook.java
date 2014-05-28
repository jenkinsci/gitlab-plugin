package com.dabsquared.gitlabjenkins;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.*;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.security.csrf.CrumbExclusion;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.HttpResponse;

/**
 *
 * @author Daniel Brooks
 */

@Extension
public class GitLabWebHook implements UnprotectedRootAction {

    public static final String WEBHOOK_URL = "project";

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return WEBHOOK_URL;
    }

    public void getDynamic(String projectID, StaplerRequest req,StaplerResponse res) {
        LOGGER.log(Level.FINE, "WebHook called.");

        String path = req.getRestOfPath();

        String[] splitURL = path.split("/");

        List<String> paths = new LinkedList<String>(Arrays.asList(splitURL));
        if(paths.size() > 0 && paths.get(0).equals("")) {
            paths.remove(0); //The first split is usually blank so we remove it.
        }


        String lastPath = paths.get(paths.size()-1);

        String token = req.getParameter("token");


        //TODO: Check token authentication with project id.

        if(lastPath.equals("status.json")) {
            String commitSHA1 = paths.get(1);

            //TODO: Show the status of the build. See: https://github.com/fcelda/gitlab2jenkins/blob/master/web.rb#L71
        }else if(lastPath.equals("build")) {
            String force = req.getParameter("force");

            //TODO: Parse the body and build. See: https://github.com/fcelda/gitlab2jenkins/blob/master/web.rb#L99
        } else if(lastPath.equals("refresh")) {

            //TODO: Refresh builds? See: https://github.com/fcelda/gitlab2jenkins/blob/master/web.rb#L148
        }

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
                trigger.onPost(req);
            }
        } finally {
            SecurityContextHolder.getContext().setAuthentication(old);
        }
    }


    private static final Logger LOGGER = Logger.getLogger(GitLabWebHook.class.getName());

}
