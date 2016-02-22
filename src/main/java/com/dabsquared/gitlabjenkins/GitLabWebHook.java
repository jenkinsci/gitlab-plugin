package com.dabsquared.gitlabjenkins;

import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.data.LastCommit;
import com.dabsquared.gitlabjenkins.data.ObjectAttributes;
import com.dabsquared.gitlabjenkins.webhook.status.BranchBuildPageRedirectAction;
import com.dabsquared.gitlabjenkins.webhook.status.BranchStatusPngAction;
import com.dabsquared.gitlabjenkins.webhook.status.CommitBuildPageRedirectAction;
import com.dabsquared.gitlabjenkins.webhook.status.StatusJsonAction;
import com.dabsquared.gitlabjenkins.webhook.status.CommitStatusPngAction;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import hudson.Extension;
import hudson.model.*;
import hudson.plugins.git.Branch;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import hudson.plugins.git.util.MergeRecord;
import hudson.security.ACL;
import hudson.security.csrf.CrumbExclusion;
import hudson.triggers.Trigger;
import hudson.util.HttpResponses;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONObject;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabCommit;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        LOGGER.log(Level.INFO, "WebHook called with url: {0}", req.getRestOfPath());
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

        /*
         * Since GitLab 7.10 the URL contains the pushed branch name.
         * Extract and store the branch name for further processing.
         * http://jenkins.host.com/project/<ProjectName>/refs/<branchName>/commit/<SHA1>
         */
        String sourceBranch = null;
        if (!paths.isEmpty() && paths.get(0).equals("refs")) {
            int index = paths.lastIndexOf("commits");
            if (index == -1)
                index = paths.lastIndexOf("builds");
            if (index == -1)
                index = paths.lastIndexOf("!builds");
            
            if (index > 1) {
                sourceBranch = Joiner.on('/').join(paths.subList(1, index)); // extract branch
                paths.subList(0, index).clear(); // remove 'refs/<branchName>'
            }
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
        	if (req.hasParameter("ref")){
                new BranchBuildPageRedirectAction(project, req.getParameter("ref")).execute(res);
        	} else {
        		this.generateBuild(theString, project, req, res);           
        	}
        	throw HttpResponses.ok();
        }

        String lastPath = paths.get(paths.size()-1);
        String firstPath = paths.get(0);
        if(lastPath.equals("status.json") && !firstPath.equals("!builds")) {
            new StatusJsonAction(project, paths.get(1)).execute(res);
        } else if(lastPath.equals("build") || (lastPath.equals("status.json") && firstPath.equals("!builds"))) {
            this.generateBuild(theString, project, req, res);
        } else if(lastPath.equals("status.png")) {
            if (req.hasParameter("ref")) {
                new BranchStatusPngAction(project, req.getParameter("ref")).execute(res);
            } else {
                new CommitStatusPngAction(project, req.getParameter("sha1")).execute(res);
            }
        } else if((firstPath.equals("commits") || firstPath.equals("builds")) && !lastPath.equals("status.json")) {
            new CommitBuildPageRedirectAction(project, lastPath).execute(res);
        } else{
            LOGGER.warning("Dynamic request mot met: First path: '" + firstPath + "' late path: '" + lastPath + "'");
        }

        throw HttpResponses.ok();

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
    private void generateBuild(String data, Job project, StaplerRequest req, StaplerResponse rsp) {
        JSONObject json = JSONObject.fromObject(data);
        LOGGER.log(Level.FINE, "data: {0}", json.toString(4));

        String objectType = json.optString("object_kind");

        if(objectType != null && objectType.equals("merge_request")) {
            this.generateMergeRequestBuild(data, project, req, rsp);
        } else {
            this.generatePushBuild(data, project, req, rsp);
        }
    }


    public void generatePushBuild(String json, Job project, StaplerRequest req, StaplerResponse rsp) {
        GitLabPushRequest request = GitLabPushRequest.create(json);
        String repositoryUrl = request.getRepository().getUrl();
        if (repositoryUrl == null) {

            LOGGER.log(Level.WARNING, "No repository url found.");
            return;
        }

        Authentication old = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
        try {

            GitLabPushTrigger trigger = null;
            if (project instanceof ParameterizedJobMixIn.ParameterizedJob) {
                ParameterizedJobMixIn.ParameterizedJob p = (ParameterizedJobMixIn.ParameterizedJob) project;
                for (Trigger t : p.getTriggers().values()) {

                    if (t instanceof GitLabPushTrigger) {
                        trigger = (GitLabPushTrigger) t;
                    }
                }
            }

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

            if (!trigger.getTriggerOpenMergeRequestOnPush().equals("never")) {
            	// Fetch and build open merge requests with the same source branch
            	buildOpenMergeRequests(trigger, request.getProject_id(), request.getRef(), project);
            }
        } finally {
            SecurityContextHolder.getContext().setAuthentication(old);
        }
    }

	protected void buildOpenMergeRequests(GitLabPushTrigger trigger, Integer projectId, String projectRef, Job<?, ?> project) {
		try {
            GitLabConnectionProperty property = project.getProperty(GitLabConnectionProperty.class);
            if (property != null && property.getClient() != null) {
                GitlabAPI client = property.getClient();
                List<GitlabMergeRequest> mergeRequests = client.getOpenMergeRequests(projectId);

                for (org.gitlab.api.models.GitlabMergeRequest mr : mergeRequests) {
                    if (projectRef.endsWith(mr.getSourceBranch()) ||
                            (trigger.getTriggerOpenMergeRequestOnPush().equals("both") && projectRef.endsWith(mr.getTargetBranch()))) {

                        if (trigger.getCiSkip() && mr.getDescription().contains("[ci-skip]")) {
                            LOGGER.log(Level.INFO, "Skipping MR " + mr.getTitle() + " due to ci-skip.");
                            continue;
                        }
                        GitlabBranch branch = client.getBranch(client.getProject(projectId), mr.getSourceBranch());
                        LastCommit lastCommit = new LastCommit();
                        lastCommit.setId(branch.getCommit().getId());
                        lastCommit.setMessage(branch.getCommit().getMessage());
                        lastCommit.setUrl(GitlabProject.URL + "/" + projectId + "/repository" + GitlabCommit.URL + "/"
                                + branch.getCommit().getId());

                        LOGGER.log(Level.FINE,
                                "Generating new merge trigger from "
                                        + mr.toString() + "\n source: "
                                        + mr.getSourceBranch() + "\n target: "
                                        + mr.getTargetBranch() + "\n state: "
                                        + mr.getState() + "\n assign: "
                                        + (mr.getAssignee() != null ? mr.getAssignee().getName() : "") + "\n author: "
                                        + (mr.getAuthor() != null ? mr.getAuthor().getName() : "") + "\n id: "
                                        + mr.getId() + "\n iid: "
                                        + mr.getIid() + "\n last commit: "
                                        + lastCommit.getId() + "\n\n");
                        GitLabMergeRequest newReq = new GitLabMergeRequest();
                        newReq.setObject_kind("merge_request");
                        newReq.setObjectAttribute(new ObjectAttributes());
                        if (mr.getAssignee() != null)
                            newReq.getObjectAttribute().setAssignee(mr.getAssignee());
                        if (mr.getAuthor() != null)
                            newReq.getObjectAttribute().setAuthor(mr.getAuthor());
                        newReq.getObjectAttribute().setDescription(mr.getDescription());
                        newReq.getObjectAttribute().setId(mr.getId());
                        newReq.getObjectAttribute().setIid(mr.getIid());
                        newReq.getObjectAttribute().setMergeStatus(mr.getState());
                        newReq.getObjectAttribute().setSourceBranch(mr.getSourceBranch());
                        newReq.getObjectAttribute().setSourceProjectId(mr.getSourceProjectId());
                        newReq.getObjectAttribute().setTargetBranch(mr.getTargetBranch());
                        newReq.getObjectAttribute().setTargetProjectId(projectId);
                        newReq.getObjectAttribute().setTitle(mr.getTitle());
                        newReq.getObjectAttribute().setLastCommit(lastCommit);

                        Authentication old = SecurityContextHolder.getContext().getAuthentication();
                        SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
                        try {
                            trigger.onPost(newReq);
                        } finally {
                            SecurityContextHolder.getContext().setAuthentication(old);
                        }
                    }
                }
            }
		} catch (Exception e) {
			LOGGER.warning("failed to communicate with gitlab server to determine is this is an update for a merge request: "
					+ e.getMessage());
			e.printStackTrace();
		}
	}

    public void generateMergeRequestBuild(String json, Job project, StaplerRequest req, StaplerResponse rsp) {
        GitLabMergeRequest request = GitLabMergeRequest.create(json);
        if("closed".equals(request.getObjectAttribute().getState())) {
            LOGGER.log(Level.INFO, "Closed Merge Request, no build started");
            return;
        }
        if("merged".equals(request.getObjectAttribute().getState())) {
            LOGGER.log(Level.INFO, "Accepted Merge Request, no build started");
            return;
        }
        if("update".equals(request.getObjectAttribute().getAction())) {
            LOGGER.log(Level.INFO, "Existing Merge Request, build will be trigged by buildOpenMergeRequests instead");
            return;
        }
        if(request.getObjectAttribute().getLastCommit()!=null) {
            Run mergeBuild = getBuildBySHA1(project, request.getObjectAttribute().getLastCommit().getId(), true);
            if (mergeBuild != null) {
                StringParameterValue mergeBuildTargetBranch = (StringParameterValue) mergeBuild.getAction(ParametersAction.class).getParameter("gitlabTargetBranch");
                boolean targetBranchesEqual = StringUtils.equals(mergeBuildTargetBranch.value, request.getObjectAttribute().getTargetBranch());
                LOGGER.fine("Previous build's target-branch: " + mergeBuildTargetBranch.value
                        + ", current build's target-branch: "
                        + request.getObjectAttribute().getTargetBranch() + ", equals: "
                        + targetBranchesEqual);

                if (targetBranchesEqual) {
                    LOGGER.log(Level.INFO, "Last commit in Merge Request has already been built in build #" + mergeBuild.getId());
                    return;
                }
            }
        }

        Authentication old = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
        try {
            GitLabPushTrigger trigger = null;
            if (project instanceof ParameterizedJobMixIn.ParameterizedJob) {
                ParameterizedJobMixIn.ParameterizedJob p = (ParameterizedJobMixIn.ParameterizedJob) project;
                for (Trigger t : p.getTriggers().values()) {
                    if (t instanceof GitLabPushTrigger) {
                        trigger = (GitLabPushTrigger) t;
                    }
                }
            }
            if (trigger == null) {
                return;
            }

            if(trigger.getCiSkip() && request.getObjectAttribute().getDescription().contains("[ci-skip]")) {
                LOGGER.log(Level.INFO, "Skipping MR " + request.getObjectAttribute().getTitle() + " due to ci-skip.");
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
    private Run getBuildBySHA1(Job project, String commitSHA1, boolean triggeredByMergeRequest) {
        List<Run> builds = project.getBuilds();
        for(Run build : builds) {
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

    private boolean hasBeenBuilt(BuildData data, ObjectId sha1, Run build) {
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
