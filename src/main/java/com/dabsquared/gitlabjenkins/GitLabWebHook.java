package com.dabsquared.gitlabjenkins;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.*;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.util.*;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.security.csrf.CrumbExclusion;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.acegisecurity.AccessDeniedException;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContext;
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
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
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

    private static final Logger LOGGER = Logger.getLogger(GitLabWebHook.class.getName());

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

    public void getDynamic(String projectName, StaplerRequest req, StaplerResponse res) {
        LOGGER.log(Level.FINE, "WebHook called.");

        String path = req.getRestOfPath();

        String[] splitURL = path.split("/");

        List<String> paths = new LinkedList<String>(Arrays.asList(splitURL));
        if(paths.size() > 0 && paths.get(0).equals("")) {
            paths.remove(0); //The first split is usually blank so we remove it.
        }


        String lastPath = paths.get(paths.size()-1);

        String token = req.getParameter("token");

        //TODO: Check token authentication with project id. For now we are not using this.

        AbstractProject project = null;
        try {
            project = project(projectName, req, res);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "no such job {0}", projectName);
            throw HttpResponses.notFound();
        }

        if(lastPath.equals("status.json")) {
            String commitSHA1 = paths.get(1);
            this.generateStatusJSON(commitSHA1, project, req, res);
        } else if(lastPath.equals("build")) {
            String force = req.getParameter("force");

            //TODO: Parse the body and build. See: https://github.com/fcelda/gitlab2jenkins/blob/master/web.rb#L99
        } else if(lastPath.equals("status.png")) {
            String branch = req.getParameter("ref");
            try {
                this.generateStatusPNG(branch, project, req, res);
            } catch (ServletException e) {
                e.printStackTrace();
                throw HttpResponses.error(500,"Could not generate an image.");
            } catch (IOException e) {
                e.printStackTrace();
                throw HttpResponses.error(500,"Could not generate an image.");
            }
        }

    }

    private void generateStatusJSON(String commitSHA1, AbstractProject project, StaplerRequest req, StaplerResponse rsp) {
        SCM scm = project.getScm();
        if(!(scm instanceof GitSCM)) {
            throw new IllegalArgumentException("This repo does not use git.");
        }

        GitSCM git = (GitSCM) scm;
        AbstractBuild build = git.getBySHA1(commitSHA1);

    }


    private void generateStatusPNG(String branch, AbstractProject project, StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {
        SCM scm = project.getScm();
        if(!(scm instanceof GitSCM)) {
            throw new IllegalArgumentException("This repo does not use git.");
        }

        AbstractBuild mainBuild = null;

        List<AbstractBuild> builds = project.getBuilds();
        for(AbstractBuild build : builds) {
            BuildData data = build.getAction(BuildData.class);
            hudson.plugins.git.util.Build branchBuild = data.getBuildsByBranchName().get(branch);
            if(branchBuild != null) {
                int buildNumber = branchBuild.getBuildNumber();
                mainBuild = project.getBuildByNumber(buildNumber);
                break;
            }
        }

        if(mainBuild == null) {
            rsp.sendRedirect2(Jenkins.getInstance().getRootUrl() + "/plugin/gitlab-jenkins/images/unknown.png");
        }

        assert mainBuild != null;
        BallColor currentBallColor = mainBuild.getIconColor().noAnime();

        if(mainBuild.isBuilding()) {
            rsp.sendRedirect2(Jenkins.getInstance().getRootUrl() + "/plugin/gitlab-jenkins/images/running.png");
        }else if(currentBallColor == BallColor.BLUE) {
            rsp.sendRedirect2(Jenkins.getInstance().getRootUrl() + "/plugin/gitlab-jenkins/images/success.png");
        }else if(currentBallColor == BallColor.ABORTED) {
            rsp.sendRedirect2(Jenkins.getInstance().getRootUrl() + "/plugin/gitlab-jenkins/images/unknown.png");
        }else if(currentBallColor == BallColor.DISABLED) {
            rsp.sendRedirect2(Jenkins.getInstance().getRootUrl() + "/plugin/gitlab-jenkins/images/unknown.png");
        }else if(currentBallColor == BallColor.GREY) {
            rsp.sendRedirect2(Jenkins.getInstance().getRootUrl() + "/plugin/gitlab-jenkins/images/unknown.png");
        }else if(currentBallColor == BallColor.NOTBUILT) {
            rsp.sendRedirect2(Jenkins.getInstance().getRootUrl() + "/plugin/gitlab-jenkins/images/unknown.png");
        }else if(currentBallColor == BallColor.RED) {
            rsp.sendRedirect2(Jenkins.getInstance().getRootUrl() + "/plugin/gitlab-jenkins/images/failed.png");
        }else if(currentBallColor == BallColor.YELLOW) {
            rsp.sendRedirect2(Jenkins.getInstance().getRootUrl() + "/plugin/gitlab-jenkins/images/unknown.png");
        } else {
            rsp.sendRedirect2(Jenkins.getInstance().getRootUrl() + "/plugin/gitlab-jenkins/images/unknown.png");
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


    /**
     *
     * @param rsp The stapler response to write the output to.
     * @throws IOException
     */
    private  void writeJSON(StaplerResponse rsp) throws IOException {
        rsp.setContentType("text/html");
        PrintWriter w = rsp.getWriter();
        w.write("Scheduled.\n");
        w.close();
    }


    /**
     *
     * @param job The job name
     * @param req The stapler request asking for the project
     * @param rsp The stapler response asking for the project
     * @return A project that matches the information.
     * @throws IOException
     * @throws HttpResponses.HttpResponseException
     */
    @SuppressWarnings("deprecation")
    private AbstractProject<?,?> project(String job, StaplerRequest req, StaplerResponse rsp) throws IOException, HttpResponses.HttpResponseException {
        AbstractProject<?,?> p;
        SecurityContext orig = ACL.impersonate(ACL.SYSTEM);
        try {
            p = Jenkins.getInstance().getItemByFullName(job, AbstractProject.class);
        } finally {
            SecurityContextHolder.setContext(orig);
        }
        if (p == null) {
            LOGGER.log(Level.FINE, "no such job {0}", job);
            throw HttpResponses.notFound();
        }
        return p;
    }





}
