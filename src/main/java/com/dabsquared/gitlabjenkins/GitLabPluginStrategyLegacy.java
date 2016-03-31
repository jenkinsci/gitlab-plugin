/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabsquared.gitlabjenkins;

import static com.dabsquared.gitlabjenkins.GitLabPushTrigger.getDesc;
import com.dabsquared.gitlabjenkins.data.LastCommit;
import com.dabsquared.gitlabjenkins.data.ObjectAttributes;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.plugins.git.RevisionParameterAction;
import hudson.security.ACL;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabCommit;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;

/**
 *
 * @author Pablo Bendersky
 */
public class GitLabPluginStrategyLegacy implements GitLabPluginStrategy {

    private static final Logger LOGGER = Logger.getLogger(GitLabPluginStrategyLegacy.class.getName());

    @Override
    public Action[] createActions(GitLabPushTrigger pushTrigger, GitLabPushRequest req, Job job) {
        ArrayList<Action> actions = new ArrayList<Action>();
        actions.add(new CauseAction(pushTrigger.createGitLabPushCause(req)));

        String branch = pushTrigger.getSourceBranch(req);

        LOGGER.log(Level.INFO, "GitLab Push Request from branch {0}.", branch);

        Map<String, ParameterValue> values = pushTrigger.getDefaultParameters();
        values.put("gitlabSourceBranch", new StringParameterValue("gitlabSourceBranch", branch));
        values.put("gitlabTargetBranch", new StringParameterValue("gitlabTargetBranch", branch));
        values.put("gitlabBranch", new StringParameterValue("gitlabBranch", branch));

        values.put("gitlabActionType", new StringParameterValue("gitlabActionType", "PUSH"));
        values.put("gitlabUserName", new StringParameterValue("gitlabUserName", req.getCommits().get(0).getAuthor().getName()));
        values.put("gitlabUserEmail", new StringParameterValue("gitlabUserEmail", req.getCommits().get(0).getAuthor().getEmail()));
        values.put("gitlabMergeRequestTitle", new StringParameterValue("gitlabMergeRequestTitle", ""));
        values.put("gitlabMergeRequestId", new StringParameterValue("gitlabMergeRequestId", ""));
        values.put("gitlabMergeRequestDescription", new StringParameterValue("gitlabMergeRequestDescription", ""));
        values.put("gitlabMergeRequestAssignee", new StringParameterValue("gitlabMergeRequestAssignee", ""));

        LOGGER.log(Level.INFO, "Trying to get name and URL for job: {0}", job.getFullName());
        String sourceRepoName = getDesc().getSourceRepoNameDefault(job);
        String sourceRepoURL = getDesc().getSourceRepoURLDefault(job).toString();

        if (!pushTrigger.getDescriptor().getGitlabHostUrl().isEmpty()) {
            // Get source repository if communication to Gitlab is possible
            try {
                sourceRepoName = req.getSourceProject(getDesc().getGitlab()).getPathWithNamespace();
                sourceRepoURL = req.getSourceProject(getDesc().getGitlab()).getSshUrl();
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Could not fetch source project''s data from Gitlab. '('{0}':' {1}')'", new String[]{ex.toString(), ex.getMessage()});
            }
        }

        values.put("gitlabSourceRepoName", new StringParameterValue("gitlabSourceRepoName", sourceRepoName));
        values.put("gitlabSourceRepoURL", new StringParameterValue("gitlabSourceRepoURL", sourceRepoURL));

        List<ParameterValue> listValues = new ArrayList<ParameterValue>(values.values());

        ParametersAction parametersAction = new ParametersAction(listValues);
        actions.add(parametersAction);
        RevisionParameterAction revision;

        revision = pushTrigger.createPushRequestRevisionParameter(job, req);
        if (revision == null) {
            return null;
        }

        actions.add(revision);
        Action[] actionsArray = actions.toArray(new Action[0]);

        return actionsArray;
    }

    @Override
    public Action createAction(GitLabPushTrigger pushTrigger, GitLabMergeRequest req, Job job) {
        Map<String, ParameterValue> values = pushTrigger.getDefaultParameters();
        values.put("gitlabSourceBranch", new StringParameterValue("gitlabSourceBranch", pushTrigger.getSourceBranch(req)));
        values.put("gitlabTargetBranch", new StringParameterValue("gitlabTargetBranch", req.getObjectAttribute().getTargetBranch()));
        values.put("gitlabActionType", new StringParameterValue("gitlabActionType", "MERGE"));
        if (req.getObjectAttribute().getAuthor() != null) {
            values.put("gitlabUserName", new StringParameterValue("gitlabUserName", req.getObjectAttribute().getAuthor().getName()));

            String email = req.getObjectAttribute().getAuthor().getEmail();
            if (email != null) {
                values.put("gitlabUserEmail", new StringParameterValue("gitlabUserEmail", email));
            }
        }
        values.put("gitlabMergeRequestTitle", new StringParameterValue("gitlabMergeRequestTitle", req.getObjectAttribute().getTitle()));
        values.put("gitlabMergeRequestId", new StringParameterValue("gitlabMergeRequestId", req.getObjectAttribute().getIid().toString()));
        values.put("gitlabMergeRequestDescription", new StringParameterValue("gitlabMergeRequestDescription", req.getObjectAttribute().getDescription()));
        if (req.getObjectAttribute().getAssignee() != null) {
            values.put("gitlabMergeRequestAssignee", new StringParameterValue("gitlabMergeRequestAssignee", req.getObjectAttribute().getAssignee().getName()));
        }

        LOGGER.log(Level.INFO, "Trying to get name and URL for job: {0}", job.getFullName());
        String sourceRepoName = getDesc().getSourceRepoNameDefault(job);
        String sourceRepoURL = getDesc().getSourceRepoURLDefault(job).toString();

        if (!pushTrigger.getDescriptor().getGitlabHostUrl().isEmpty()) {
            // Get source repository if communication to Gitlab is possible
            try {
                sourceRepoName = req.getSourceProject(getDesc().getGitlab()).getPathWithNamespace();
                sourceRepoURL = req.getSourceProject(getDesc().getGitlab()).getSshUrl();
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Could not fetch source project''s data from Gitlab. '('{0}':' {1}')'", new String[]{ex.toString(), ex.getMessage()});
            }
        }

        values.put("gitlabSourceRepoName", new StringParameterValue("gitlabSourceRepoName", sourceRepoName));
        values.put("gitlabSourceRepoURL", new StringParameterValue("gitlabSourceRepoURL", sourceRepoURL));

        List<ParameterValue> listValues = new ArrayList<ParameterValue>(values.values());

        return new ParametersAction(listValues);
    }

    @Override
    public void buildOpenMergeRequestTriggeredByPush(GitLabWebHook webHook, GitLabPushTrigger trigger, Integer projectId, String projectRef) {
        try {
            GitLab api = new GitLab();
            List<GitlabMergeRequest> mergeRequests = api.instance().getOpenMergeRequests(projectId);

            for (org.gitlab.api.models.GitlabMergeRequest mr : mergeRequests) {
                if (projectRef.endsWith(mr.getSourceBranch())
                        || (trigger.getTriggerOpenMergeRequestOnPush().equals("both") && projectRef.endsWith(mr.getTargetBranch()))) {

                    if (trigger.getCiSkip() && mr.getDescription().contains("[ci-skip]")) {
                        LOGGER.log(Level.INFO, "Skipping MR " + mr.getTitle() + " due to ci-skip.");
                        continue;
                    }

                    Integer srcProjectId = projectId;
                    if (!projectRef.endsWith(mr.getSourceBranch())) {
                        srcProjectId = mr.getSourceProjectId();
                    }

                    GitlabBranch branch = api.instance().getBranch(api.instance().getProject(srcProjectId), mr.getSourceBranch());
                    LastCommit lastCommit = new LastCommit();
                    lastCommit.setId(branch.getCommit().getId());
                    lastCommit.setMessage(branch.getCommit().getMessage());
                    lastCommit.setUrl(GitlabProject.URL + "/" + srcProjectId + "/repository" + GitlabCommit.URL + "/"
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
                    if (mr.getAssignee() != null) {
                        newReq.getObjectAttribute().setAssignee(mr.getAssignee());
                    }
                    if (mr.getAuthor() != null) {
                        newReq.getObjectAttribute().setAuthor(mr.getAuthor());
                    }
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
        } catch (Exception e) {
            LOGGER.warning("failed to communicate with gitlab server to determine is this is an update for a merge request: "
                    + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean shouldSkipMergeRequestBuild(GitLabPushTrigger trigger, String state) {
        String[] skipStates = {"closed", "merged", "update", "opened" };

        return ArrayUtils.contains(skipStates, state);
    }

    @Override
    public boolean isMergeBuildAlreadyBuilt(GitLabMergeRequest request, Run mergeBuild) {
        StringParameterValue mergeBuildTargetBranch = (StringParameterValue) mergeBuild.getAction(ParametersAction.class).getParameter("gitlabTargetBranch");
        boolean targetBranchesEqual = StringUtils.equals(mergeBuildTargetBranch.value, request.getObjectAttribute().getTargetBranch());
        LOGGER.fine("Previous build's target-branch: " + mergeBuildTargetBranch.value
                + ", current build's target-branch: "
                + request.getObjectAttribute().getTargetBranch() + ", equals: "
                + targetBranchesEqual);

        if (targetBranchesEqual) {
            return true;
        }
        return false;
    }

    @Override
    public boolean skipsExistingPushBuildsMatchingBySHA1() {
        return false;
    }

}
