package com.dabsquared.gitlabjenkins;

import hudson.Extension;
import hudson.model.BallColor;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.UnprotectedRootAction;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.plugins.git.Branch;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import hudson.plugins.git.util.MergeRecord;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.security.csrf.CrumbExclusion;
import hudson.util.HttpResponses;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.dabsquared.gitlabjenkins.GitLabMergeRequest;
import com.dabsquared.gitlabjenkins.GitLabPushRequest;
import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
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
        LOGGER.log(Level.WARNING, "WebHook called.");
        final Iterator<String> restOfPathParts = Splitter.on('/').omitEmptyStrings().split(req.getRestOfPath()).iterator();
        final AbstractProject<?, ?>[] projectHolder = new AbstractProject<?, ?>[] { null };
        ACL.impersonate(ACL.SYSTEM, new Runnable() {

            public void run() {
                final Jenkins jenkins = Jenkins.getInstance();
                if (jenkins != null) {
                    Item item = jenkins.getItemByFullName(projectName);
                    while (item instanceof ItemGroup<?> && !(item instanceof AbstractProject<?, ?>) && restOfPathParts.hasNext()) {
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
        	if (req.getParameter("ref") != null){
        		// support /project/PROJECT_NAME?ref=BRANCH_NAME
        		// link on project activity page - build status
        		AbstractBuild build = this.getBuildByBranch(project, req.getParameter("ref"));
        		redirectToBuildPage(res, build);
        	} else {
        		this.generateBuild(theString, project, req, res);           
        	}
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
        } else if((firstPath.equals("commits") || firstPath.equals("builds")) && !lastPath.equals("status.json")) {
            AbstractBuild build = this.getBuildBySHA1(project, lastPath, true);
            redirectToBuildPage(res, build);
        } else{
            LOGGER.warning("Dynamic request mot met: First path: '" + firstPath + "' late path: '" + lastPath + "'");
        }

        throw HttpResponses.ok();

    }

	private void redirectToBuildPage(StaplerResponse res, AbstractBuild build) {
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

        //TODO: add status of pending when we figure it out.
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


    private void generateStatusPNG(String branch, String commitSHA1, AbstractProject project, final StaplerRequest req, final StaplerResponse rsp) throws ServletException, IOException {
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
        String imageUrl = "images/unknown.png";
        if(null != mainBuild) {
        	BallColor currentBallColor = mainBuild.getIconColor().noAnime();

            if(mainBuild.isBuilding()) {        	
            	imageUrl = "images/running.png";
            }else if(currentBallColor == BallColor.BLUE) {
            	imageUrl = "images/success.png";
            }else if(currentBallColor == BallColor.RED) {
            	imageUrl = "images/failed.png";
            }else if(currentBallColor == BallColor.YELLOW) {
            	imageUrl = "images/unstable.png";
            }else {
            	imageUrl = "images/unknown.png"; 
            }
        }       
        Authentication old = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
        try {
            URL resourceUrl = new URL(Jenkins.getInstance().getPlugin("gitlab-plugin").getWrapper().baseResourceURL + imageUrl);
            LOGGER.info("serving image "+resourceUrl.toExternalForm());
            rsp.setHeader("Expires","Fri, 01 Jan 1984 00:00:00 GMT");
            rsp.setHeader("Cache-Control", "no-cache, private");
            rsp.setHeader("Content-Type", "image/png");
            hudson.util.IOUtils.copy(new File(resourceUrl.toURI()), rsp.getOutputStream());
            rsp.flushBuffer();
        } catch (Exception e) {
			throw HttpResponses.error(500,"Could not generate response.");
		} finally {
            SecurityContextHolder.getContext().setAuthentication(old);
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

            if(trigger.getCiSkip() && request.getLastCommit() != null) {
                if(request.getLastCommit().getMessage().contains("[ci-skip]")) {
                    LOGGER.log(Level.INFO, "Skipping due to ci-skip.");
                    return;
                }
            }

            trigger.onPost(request);

            if (trigger.getTriggerOpenMergeRequestOnPush()) {
            	// Fetch and build open merge requests with the same source branch
            	buildOpenMergeRequests(trigger, request.getProject_id(), request.getRef());
            }
        } finally {
            SecurityContextHolder.getContext().setAuthentication(old);
        }
    }

	protected void buildOpenMergeRequests(GitLabPushTrigger trigger, Integer projectId, String projectRef) {
		try {
			GitLab api = new GitLab();
			List<org.gitlab.api.models.GitlabMergeRequest> reqs = api.instance().getMergeRequests(projectId);
			for (org.gitlab.api.models.GitlabMergeRequest mr : reqs) {
				if (!mr.isClosed() && !mr.isMerged()&& projectRef.endsWith(mr.getSourceBranch())) {
					LOGGER.log(Level.FINE,
							"Generating new merge trigger from "
									+ mr.toString() + "\n source: "
									+ mr.getSourceBranch() + "\n target: "
									+ mr.getTargetBranch() + "\n state: "
									+ mr.getState() + "\n assign: "
									+ mr.getAssignee() + "\n author: "
									+ mr.getAuthor() + "\n id: " + mr.getId()
									+ "\n iid: " + mr.getIid() + "\n\n");
					GitLabMergeRequest newReq = new GitLabMergeRequest();
					newReq.setObject_kind("merge_request");
					newReq.setObjectAttribute(new GitLabMergeRequest.ObjectAttributes());
					if (mr.getAssignee() != null)
						newReq.getObjectAttribute().setAssigneeId(mr.getAssignee().getId());
					if (mr.getAuthor() != null)
						newReq.getObjectAttribute().setAuthorId(mr.getAuthor().getId());
					newReq.getObjectAttribute().setDescription(mr.getDescription());
					newReq.getObjectAttribute().setId(mr.getId());
					newReq.getObjectAttribute().setIid(mr.getIid());
					newReq.getObjectAttribute().setMergeStatus(mr.getState());
					newReq.getObjectAttribute().setSourceBranch(mr.getSourceBranch());
					newReq.getObjectAttribute().setSourceProjectId(mr.getSourceProjectId());
					newReq.getObjectAttribute().setTargetBranch(mr.getTargetBranch());
					newReq.getObjectAttribute().setTargetProjectId(projectId);
					newReq.getObjectAttribute().setTitle(mr.getTitle());

					Authentication old = SecurityContextHolder.getContext().getAuthentication();
					SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
					try {
						trigger.onPost(newReq);
					} finally {
						SecurityContextHolder.getContext().setAuthentication(old);
					}
					return;
				}
			}
		} catch (Exception e) {
			LOGGER.warning("failed to communicate with gitlab server to determine is this is an update for a merge request: "
					+ e.getMessage());
			e.printStackTrace();
		}
	}

    public void generateMergeRequestBuild(String json, AbstractProject project, StaplerRequest req, StaplerResponse rsp) {
        GitLabMergeRequest request = GitLabMergeRequest.create(json);
        if(request.getObjectAttribute().getState().equals("closed")) {
        	LOGGER.log(Level.INFO, "Closed Merge Request, no build started");
            return;
        }
        if(request.getObjectAttribute().getState().equals("merged")) {
        	LOGGER.log(Level.INFO, "Accepted Merge Request, no build started");
            return;
        }
        AbstractBuild mergeBuild = getBuildBySHA1(project, request.getObjectAttribute().getLastCommit().getId(), true);
        if(mergeBuild!=null){
            LOGGER.log(Level.INFO, "Last commit in Merge Request has already been build in build #"+mergeBuild.getId());
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
    private AbstractBuild getBuildBySHA1(AbstractProject project, String commitSHA1, boolean triggeredByMergeRequest) {
        List<AbstractBuild> builds = project.getBuilds();
        for(AbstractBuild build : builds) {
            BuildData data = build.getAction(BuildData.class);
            MergeRecord mergeRecord = build.getAction(MergeRecord.class);
            if (mergeRecord == null) {
                //Determine if build was triggered by a Merge Request event
                ParametersAction params = build.getAction(ParametersAction.class);

                if (params == null) continue;

                StringParameterValue sourceBranch = (StringParameterValue) params.getParameter("gitlabSourceBranch");
                StringParameterValue targetBranch = (StringParameterValue) params.getParameter("gitlabTargetBranch");
                boolean isMergeRequestBuild = (sourceBranch != null && !sourceBranch.value.equals(targetBranch.value));

                if (!triggeredByMergeRequest) {
    				if (isMergeRequestBuild)
    					// skip Merge Request builds
    					continue;

                    if (data.getLastBuiltRevision().getSha1String().contains(commitSHA1)) {
                        return build;
                    }
                } else {
    				if (!isMergeRequestBuild)
    					// skip Push builds
    					continue;

    				if (hasBeenBuilt(data, ObjectId.fromString(commitSHA1), build)) {
    					return build;
    				}
                }

            } else {
            	Build b =  data.lastBuild;
            	boolean isMergeBuild = mergeRecord!=null && !mergeRecord.getSha1().equals(b.getMarked().getSha1String());
            	if(b!=null && b.getMarked()!=null && b.getMarked().getSha1String().equals(commitSHA1)){
            		if(triggeredByMergeRequest == isMergeBuild){
            			LOGGER.log(Level.FINE, build.getNumber()+" Build found matching "+commitSHA1+" "+(isMergeBuild? "merge":"normal")+" build");
            			return build;
            		}
            	}
            }
        }
        return null;
    }

    private boolean hasBeenBuilt(BuildData data, ObjectId sha1, AbstractBuild build) {
		try {
			for (Build b : data.getBuildsByBranchName().values()) {
				if (b.getBuildNumber() == build.number
						&& b.marked.getSha1().equals(sha1))
					return true;
			}
			return false;
		} catch (Exception ex) {
			return false;
		}
	}
    
    /**
     *
     * @param project
     * @param branch
     * @return latest build of the branch specified that is not part of a merge request
     */
    @SuppressWarnings("rawtypes")
	private AbstractBuild getBuildByBranch(AbstractProject project, String branch) {
        List<AbstractBuild> builds = project.getBuilds();
        for(AbstractBuild build : builds) {
            BuildData data = build.getAction(BuildData.class);
            if(data!=null && data.lastBuild!=null) {
                MergeRecord merge = build.getAction(MergeRecord.class);
                boolean isMergeBuild = merge != null && !merge.getSha1().equals(data.lastBuild.getMarked().getSha1String());
                if (data.lastBuild.getRevision() != null && !isMergeBuild) {
                    for (Branch b : data.lastBuild.getRevision().getBranches()) {
                        if (b.getName().endsWith("/" + branch))
                            return build;
                    }
                }
            }
        }
        return null;
    }


    /**
     *
     * @param rsp The stapler response to write the output to.
     * @throws IOException
     */
    private void writeJSON(StaplerResponse rsp, JSONObject jsonObject) throws IOException {
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

    @Extension
    public static class GitlabWebHookCrumbExclusion extends CrumbExclusion {

        @Override
        public boolean process(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {
            String pathInfo = req.getPathInfo();
            if (pathInfo != null && pathInfo.startsWith(getExclusionPath())) {
                chain.doFilter(req, resp);
                return true;
            }
            return false;
        }

        private String getExclusionPath() {
            return '/' + WEBHOOK_URL + '/';
        }
    }
}
