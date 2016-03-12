package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.model.PushHook;
import com.dabsquared.gitlabjenkins.util.GsonUtil;
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

import static com.dabsquared.gitlabjenkins.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.model.builder.generated.MergeRequestHookBuilder.mergeRequestHook;
import static com.dabsquared.gitlabjenkins.model.builder.generated.ObjectAttributesBuilder.objectAttributes;
import static com.dabsquared.gitlabjenkins.util.GsonUtil.toPrettyPrint;

/**
 * @author Robin Müller
 */
public class PushBuildAction implements WebHookAction {

    private final static Logger LOGGER = Logger.getLogger(PushBuildAction.class.getName());
    private final AbstractProject<?, ?> project;

    private PushHook pushHook;

    public PushBuildAction(AbstractProject<?, ?> project, String json) {
        LOGGER.log(Level.FINE, "Push: {0}", toPrettyPrint(json));
        this.project = project;
        this.pushHook = GsonUtil.getGson().fromJson(json, PushHook.class);
    }

    public void execute(StaplerResponse response) {
        String repositoryUrl = pushHook.getRepository().getUrl();
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
                    trigger.onPost(pushHook);

                    if (!trigger.getTriggerOpenMergeRequestOnPush().equals("never")) {
                        // Fetch and build open merge requests with the same source branch
                        buildOpenMergeRequests(trigger, pushHook.getProjectId(), pushHook.getRef());
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
                final GitlabAPI client = property.getClient();
                for (final GitlabMergeRequest mergeRequest : client.getOpenMergeRequests(projectId)) {
                    final String sourceBranch = mergeRequest.getSourceBranch();
                    String targetBranch = mergeRequest.getTargetBranch();
                    if (projectRef.endsWith(sourceBranch) || (trigger.getTriggerOpenMergeRequestOnPush().equals("both") && projectRef.endsWith(targetBranch))) {
                        if (trigger.getCiSkip() && mergeRequest.getDescription().contains("[ci-skip]")) {
                            LOGGER.log(Level.INFO, "Skipping MR " + mergeRequest.getTitle() + " due to ci-skip.");
                            continue;
                        }
                        final GitlabBranch branch = client.getBranch(createProject(projectId), sourceBranch);
                        ACL.impersonate(ACL.SYSTEM, new Runnable() {
                            public void run() {
                                trigger.onPost(createMergeRequest(projectId, mergeRequest, branch));
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

    private MergeRequestHook createMergeRequest(Integer projectId, GitlabMergeRequest mergeRequest, GitlabBranch branch) {
        return mergeRequestHook()
                .withObjectKind("merge_request")
                .withObjectAttributes(objectAttributes()
                        .withAssigneeId(mergeRequest.getAssignee() == null ? null : mergeRequest.getAssignee().getId())
                        .withAuthorId(mergeRequest.getAuthor().getId())
                        .withDescription(mergeRequest.getDescription())
                        .withId(mergeRequest.getId())
                        .withIid(mergeRequest.getIid())
                        .withMergeStatus(mergeRequest.getState())
                        .withSourceBranch(mergeRequest.getSourceBranch())
                        .withSourceProjectId(mergeRequest.getSourceProjectId())
                        .withTargetBranch(mergeRequest.getTargetBranch())
                        .withTargetProjectId(projectId)
                        .withTitle(mergeRequest.getTitle())
                        .withLastCommit(commit()
                                .withId(branch.getCommit().getId())
                                .withMessage(branch.getCommit().getMessage())
                                .withUrl(GitlabProject.URL + "/" + projectId + "/repository" + GitlabCommit.URL + "/" + branch.getCommit().getId())
                                .build())
                        .build())
                .build();
    }

    private boolean isLastBuildCiSkip() {
        return !pushHook.getCommits().isEmpty() && pushHook.getCommits().get(0).getMessage().contains("[ci-skip]");
    }
}
