package com.dabsquared.gitlabjenkins;

import hudson.Extension;
import hudson.model.BallColor;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.UnprotectedRootAction;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.util.BuildData;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.util.HttpResponses;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.google.common.base.Splitter;

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

    public void getDynamic(final String projectName, final StaplerRequest req, StaplerResponse res) {
        LOGGER.log(Level.FINE, "WebHook called.");
        final Iterator<String> restOfPathParts = Splitter.on('/').omitEmptyStrings().split(req.getRestOfPath()).iterator();
        final AbstractProject<?, ?>[] projectHolder = new AbstractProject<?, ?>[] { null };
        ACL.impersonate(ACL.SYSTEM, new Runnable() {

            public void run() {
                final Jenkins jenkins = Jenkins.getInstance();
                if (jenkins != null) {
                    Item item = jenkins.getItemByFullName(projectName);
                    while (item instanceof ItemGroup<?> && restOfPathParts.hasNext()) {
                        item = jenkins.getItem(restOfPathParts.next(), (ItemGroup<?>) item);
                    }
                    if (item instanceof AbstractProject<?, ?>) {
                        projectHolder[0] = (AbstractProject<?, ?>) item;
                    }
                }
            }

        });

        final AbstractProject<?, ?> project = projectHolder[0];
        if (project == null) {
            throw HttpResponses.notFound();
        }

        final List<String> paths = new ArrayList<String>();
        while (restOfPathParts.hasNext()) {
            paths.add(restOfPathParts.next());
        }

        String token = req.getParameter("token");

        //TODO: Check token authentication with project id. For now we are not using this.

        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(req.getInputStream(), writer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String theString = writer.toString();

        if(paths.size() == 0) {
            this.generateBuild(theString, project, req, res);
            throw HttpResponses.ok();
        }

        String lastPath = paths.get(paths.size()-1);
        String firstPath = paths.get(0);

        if(lastPath.equals("status.json") && !firstPath.equals("!builds")) {
            String commitSHA1 = paths.get(1);
            this.generateStatusJSON(commitSHA1, project, req, res);
        } else if(lastPath.equals("build") || (lastPath.equals("status.json") && firstPath.equals("!builds"))) {
            this.generateBuild(theString, project, req, res);
        } else if(lastPath.equals("status.png")) {
            String branch = req.getParameter("ref");
            String commitSHA1 = req.getParameter("sha1");
            try {
                this.generateStatusPNG(branch, commitSHA1, project, req, res);
            } catch (ServletException e) {
                e.printStackTrace();
                throw HttpResponses.error(500,"Could not generate an image.");
            } catch (IOException e) {
                e.printStackTrace();
                throw HttpResponses.error(500,"Could not generate an image.");
            }
        } else if(firstPath.equals("builds") && !lastPath.equals("status.json")) {
            AbstractBuild build = this.getBuildBySHA1(project, lastPath, true);
            if(build != null) {
                try {
                    res.sendRedirect2(Jenkins.getInstance().getRootUrl() + build.getUrl());
                } catch (IOException e) {
                    try {
                        res.sendRedirect2(Jenkins.getInstance().getRootUrl() + build.getBuildStatusUrl());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }

        throw HttpResponses.ok();

    }

    private void generateStatusJSON(String commitSHA1, AbstractProject project, StaplerRequest req, StaplerResponse rsp) {
        SCM scm = project.getScm();
        if(!(scm instanceof GitSCM)) {
            throw new IllegalArgumentException("This repo does not use git.");
        }

        AbstractBuild mainBuild = this.getBuildBySHA1(project, commitSHA1, true);

        JSONObject object = new JSONObject();
        object.put("sha", commitSHA1);

        if(mainBuild == null) {
            try {
                object.put("status", "pending");
                this.writeJSON(rsp, object);
                return;
            } catch (IOException e) {
                throw HttpResponses.error(500,"Could not generate response.");
            }
        }


        object.put("id", mainBuild.getNumber());

        BallColor currentBallColor = mainBuild.getIconColor().noAnime();

        //TODO: add staus of pending when we figure it out.
        if(mainBuild.isBuilding()) {
            object.put("status", "running");
        }else if(currentBallColor == BallColor.BLUE) {
            object.put("status", "success");
        }else if(currentBallColor == BallColor.ABORTED) {
            object.put("status", "failed");
        }else if(currentBallColor == BallColor.DISABLED) {
            object.put("status", "failed");
        }else if(currentBallColor == BallColor.GREY) {
            object.put("status", "failed");
        }else if(currentBallColor == BallColor.NOTBUILT) {
            object.put("status", "failed");
        }else if(currentBallColor == BallColor.RED) {
            object.put("status", "failed");
        }else if(currentBallColor == BallColor.YELLOW) {
            object.put("status", "failed");
        } else {
            object.put("status", "failed");
        }

        try {
            this.writeJSON(rsp, object);
        } catch (IOException e) {
            throw HttpResponses.error(500,"Could not generate response.");
        }
    }


    private void generateStatusPNG(String branch, String commitSHA1, AbstractProject project, StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {
        SCM scm = project.getScm();
        if(!(scm instanceof GitSCM)) {
            throw new IllegalArgumentException("This repo does not use git.");
        }

        AbstractBuild mainBuild = null;

        if(branch != null) {
            mainBuild = this.getBuildByBranch(project, branch);
        } else if(commitSHA1 != null) {
            mainBuild = this.getBuildBySHA1(project, commitSHA1, false);
        }

        String baseUrl = Jenkins.getInstance().getRootUrl();
        // Remove trailing slash
        if (baseUrl.endsWith("/")) {
           baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        if(mainBuild == null) {
            rsp.sendRedirect2(baseUrl + "/plugin/gitlab-plugin/images/unknown.png");
            return;
        }

        BallColor currentBallColor = mainBuild.getIconColor().noAnime();

        if(mainBuild.isBuilding()) {
            rsp.sendRedirect2(baseUrl + "/plugin/gitlab-plugin/images/running.png");
        }else if(currentBallColor == BallColor.BLUE) {
            rsp.sendRedirect2(baseUrl + "/plugin/gitlab-plugin/images/success.png");
        }else if(currentBallColor == BallColor.ABORTED) {
            rsp.sendRedirect2(baseUrl + "/plugin/gitlab-plugin/images/unknown.png");
        }else if(currentBallColor == BallColor.DISABLED) {
            rsp.sendRedirect2(baseUrl + "/plugin/gitlab-plugin/images/unknown.png");
        }else if(currentBallColor == BallColor.GREY) {
            rsp.sendRedirect2(baseUrl + "/plugin/gitlab-plugin/images/unknown.png");
        }else if(currentBallColor == BallColor.NOTBUILT) {
            rsp.sendRedirect2(baseUrl + "/plugin/gitlab-plugin/images/unknown.png");
        }else if(currentBallColor == BallColor.RED) {
            rsp.sendRedirect2(baseUrl + "/plugin/gitlab-plugin/images/failed.png");
        }else if(currentBallColor == BallColor.YELLOW) {
            rsp.sendRedirect2(baseUrl + "/plugin/gitlab-plugin/images/unknown.png");
        } else {
            rsp.sendRedirect2(baseUrl + "/plugin/gitlab-plugin/images/unknown.png");
        }

    }


    /**
     * Take the GitLab Data and parse through it.
     * {
     #     "before": "95790bf891e76fee5e1747ab589903a6a1f80f22",
     #     "after": "da1560886d4f094c3e6c9ef40349f7d38b5d27d7",
     #     "ref": "refs/heads/master",
     #     "commits": [
     #       {
     #         "id": "b6568db1bc1dcd7f8b4d5a946b0b91f9dacd7327",
     #         "message": "Update Catalan translation to e38cb41.",
     #         "timestamp": "2011-12-12T14:27:31+02:00",
     #         "url": "http://localhost/diaspora/commits/b6568db1bc1dcd7f8b4d5a946b0b91f9dacd7327",
     #         "author": {
     #           "name": "Jordi Mallach",
     #           "email": "jordi@softcatala.org",
     #         }
     #       }, .... more commits
     #     ]
     #   }
     * @param data
     */
    private void generateBuild(String data, AbstractProject project, StaplerRequest req, StaplerResponse rsp) {
        JSONObject json = JSONObject.fromObject(data);
        LOGGER.log(Level.FINE, "data: {0}", json.toString(4));

        String objectType = json.optString("object_kind");

        if(objectType != null && objectType.equals("merge_request")) {
            this.generateMergeRequestBuild(data, project, req, rsp);
        } else {
            this.generatePushBuild(data, project, req, rsp);
        }
    }


    public void generatePushBuild(String json, AbstractProject project, StaplerRequest req, StaplerResponse rsp) {
        GitLabPushRequest request = GitLabPushRequest.create(json);

        String repositoryUrl = request.getRepository().getUrl();
        if (repositoryUrl == null) {
            LOGGER.log(Level.WARNING, "No repository url found.");
            return;
        }

        Authentication old = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
        try {
            GitLabPushTrigger trigger = (GitLabPushTrigger) project.getTrigger(GitLabPushTrigger.class);
            if (trigger == null) {
                return;
            }
            trigger.onPost(request);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(old);
        }
    }

    public void generateMergeRequestBuild(String json, AbstractProject project, StaplerRequest req, StaplerResponse rsp) {
        GitLabMergeRequest request = GitLabMergeRequest.create(json);
        if(request.getObjectAttribute().getState().equals("closed")) {
        	LOGGER.log(Level.INFO, "Closed Merge Request, no build started");
            return;
        }

        Authentication old = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
        try {
            GitLabPushTrigger trigger = (GitLabPushTrigger) project.getTrigger(GitLabPushTrigger.class);
            if (trigger == null) {
                return;
            }
            trigger.onPost(request);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(old);
        }
    }



    /**************************************************
     *
     * Helper methods
     *
     **************************************************/


    /**
     *
     * @param project
     * @param commitSHA1
     * @return
     */
    private AbstractBuild getBuildBySHA1(AbstractProject project, String commitSHA1, boolean isMergeRequest) {
        AbstractBuild mainBuild = null;

        List<AbstractBuild> builds = project.getBuilds();
        for(AbstractBuild build : builds) {
            BuildData data = build.getAction(BuildData.class);

            if (!isMergeRequest) {
                if (data.getLastBuiltRevision().getSha1String().contains(commitSHA1)) {
                    mainBuild = build;
                    break;
                }
            } else {
                if(data.hasBeenBuilt(ObjectId.fromString(commitSHA1))) {
                    mainBuild = build;
                    break;
                }
            }
        }

        return mainBuild;
    }

    /**
     *
     * @param project
     * @param branch
     * @return
     */
    private AbstractBuild getBuildByBranch(AbstractProject project, String branch) {
        AbstractBuild mainBuild = null;

        List<AbstractBuild> builds = project.getBuilds();
        for(AbstractBuild build : builds) {
            BuildData data = build.getAction(BuildData.class);
            hudson.plugins.git.util.Build branchBuild = data.getBuildsByBranchName().get("origin/" + branch);
            if(branchBuild != null) {
                int buildNumber = branchBuild.getBuildNumber();
                mainBuild = project.getBuildByNumber(buildNumber);
                break;
            }
        }

        return mainBuild;
    }


    /**
     *
     * @param rsp The stapler response to write the output to.
     * @throws IOException
     */
    private  void writeJSON(StaplerResponse rsp, JSONObject jsonObject) throws IOException {
        rsp.setContentType("application/json");
        PrintWriter w = rsp.getWriter();

        if(jsonObject == null) {
            w.write("null");
        } else {
            w.write(jsonObject.toString());
        }

        w.flush();
        w.close();

    }

}
