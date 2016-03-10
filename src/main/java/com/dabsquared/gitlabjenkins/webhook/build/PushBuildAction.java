package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabMergeRequest;
import com.dabsquared.gitlabjenkins.GitLabPushRequest;
import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.data.LastCommit;
import com.dabsquared.gitlabjenkins.data.ObjectAttributes;
import com.dabsquared.gitlabjenkins.webhook.WebHookAction;
import hudson.model.AbstractProject;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabCommit;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;
import org.kohsuke.stapler.StaplerResponse;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Robin Müller
 */
public class PushBuildAction implements WebHookAction {

    private final static Logger LOGGER = Logger.getLogger(PushBuildAction.class.getName());
    private final AbstractProject<?, ?> project;

    private GitLabPushRequest pushRequest;

    public PushBuildAction(AbstractProject<?, ?> project, String json) {
        this.project = project;
        this.pushRequest = GitLabPushRequest.create(json);
    }

    public void execute(StaplerResponse response) {
        String repositoryUrl = pushRequest.getRepository().getUrl();
        if (repositoryUrl == null) {
            LOGGER.log(Level.WARNING, "No repository url found.");
            return;
        }

        ACL.impersonate(ACL.SYSTEM, new Runnable() {
            public void run() {
                GitLabPushTrigger trigger = project.getTrigger(GitLabPushTrigger.class);
                if (trigger != null) {
                    if (trigger.getCiSkip() && isLastBuildCiSkip()) {
                        LOGGER.log(Level.INFO, "Skipping due to ci-skip.");
                        return;
                    }
                    trigger.onPost(pushRequest);

                    if (!trigger.getTriggerOpenMergeRequestOnPush().equals("never")) {
                        // Fetch and build open merge requests with the same source branch
                        buildOpenMergeRequests(trigger, pushRequest.getProject_id(), pushRequest.getRef());
                    }
                }
            }
        });
        throw HttpResponses.ok();
    }

    protected void buildOpenMergeRequests(final GitLabPushTrigger trigger, final Integer projectId, String projectRef) {
        try {
            GitLabConnectionProperty property = project.getProperty(GitLabConnectionProperty.class);
            if (property != null && property.getClient() != null) {
                GitlabAPI client = property.getClient();
                for (final GitlabMergeRequest mergeRequest : client.getOpenMergeRequests(projectId)) {
                    String sourceBranch = mergeRequest.getSourceBranch();
                    String targetBranch = mergeRequest.getTargetBranch();
                    if (projectRef.endsWith(sourceBranch) || (trigger.getTriggerOpenMergeRequestOnPush().equals("both") && projectRef.endsWith(targetBranch))) {
                        if (trigger.getCiSkip() && mergeRequest.getDescription().contains("[ci-skip]")) {
                            LOGGER.log(Level.INFO, "Skipping MR " + mergeRequest.getTitle() + " due to ci-skip.");
                            continue;
                        }
                        final LastCommit lastCommit = createLastCommit(projectId, client.getBranch(createProject(projectId), sourceBranch));
                        ACL.impersonate(ACL.SYSTEM, new Runnable() {
                            public void run() {
                                trigger.onPost(createMergeRequest(projectId, mergeRequest, lastCommit));
                            }
                        });
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to communicate with gitlab server to determine if this is an update for a merge request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private GitlabProject createProject(Integer projectId) {
        GitlabProject project = new GitlabProject();
        project.setId(projectId);
        return project;
    }

    private LastCommit createLastCommit(Integer projectId, GitlabBranch branch) {
        LastCommit lastCommit = new LastCommit();
        lastCommit.setId(branch.getCommit().getId());
        lastCommit.setMessage(branch.getCommit().getMessage());
        lastCommit.setUrl(GitlabProject.URL + "/" + projectId + "/repository" + GitlabCommit.URL + "/"
                + branch.getCommit().getId());
        return lastCommit;
    }

    private GitLabMergeRequest createMergeRequest(Integer projectId, GitlabMergeRequest mergeRequest, LastCommit lastCommit) {
        GitLabMergeRequest result = new GitLabMergeRequest();
        result.setObject_kind("merge_request");
        result.setObjectAttribute(new ObjectAttributes());
        result.getObjectAttribute().setAssignee(mergeRequest.getAssignee());
        result.getObjectAttribute().setAuthor(mergeRequest.getAuthor());
        result.getObjectAttribute().setDescription(mergeRequest.getDescription());
        result.getObjectAttribute().setId(mergeRequest.getId());
        result.getObjectAttribute().setIid(mergeRequest.getIid());
        result.getObjectAttribute().setMergeStatus(mergeRequest.getState());
        result.getObjectAttribute().setSourceBranch(mergeRequest.getSourceBranch());
        result.getObjectAttribute().setSourceProjectId(mergeRequest.getSourceProjectId());
        result.getObjectAttribute().setTargetBranch(mergeRequest.getTargetBranch());
        result.getObjectAttribute().setTargetProjectId(projectId);
        result.getObjectAttribute().setTitle(mergeRequest.getTitle());
        result.getObjectAttribute().setLastCommit(lastCommit);
        return result;
    }

    private boolean isLastBuildCiSkip() {
        return pushRequest.getLastCommit() != null && pushRequest.getLastCommit().getMessage().contains("[ci-skip]");
    }
}
