package com.dabsquared.gitlabjenkins.listener;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.GitLabMergeCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import jenkins.model.Jenkins;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * @author Robin Müller
 */
@Extension
public class GitLabMergeRequestRunListener extends RunListener<AbstractBuild<?, ?>> {

    @Override
    public void onCompleted(AbstractBuild<?, ?> build, @Nonnull TaskListener listener) {
        GitLabPushTrigger trigger = build.getProject().getTrigger(GitLabPushTrigger.class);
        GitLabMergeCause gitLabMergeCause = build.getCause(GitLabMergeCause.class);

        if (trigger != null && gitLabMergeCause != null) {
            String buildUrl = getBuildUrl(build);
            Result buildResult = build.getResult();
            Integer projectId = gitLabMergeCause.getRequest().getObjectAttribute().getSourceProjectId();
            Integer mergeRequestId = gitLabMergeCause.getRequest().getObjectAttribute().getId();
            if (buildResult == Result.SUCCESS) {
                acceptMergeRequestIfNecessary(build, trigger, listener, projectId, mergeRequestId);
            }
            addNoteOnMergeRequestIfNecessary(build, trigger, listener, projectId, mergeRequestId, build.getProject().getDisplayName(), build.getNumber(),
                    buildUrl, getResultIcon(trigger, Result.SUCCESS), buildResult.color.getDescription());
        }
    }

    private String getBuildUrl(AbstractBuild<?, ?> build) {
        return Jenkins.getInstance().getRootUrl() + build.getUrl();
    }

    private void acceptMergeRequestIfNecessary(AbstractBuild<?, ?> build, GitLabPushTrigger trigger, TaskListener listener, Integer projectId, Integer mergeRequestId) {
        if (trigger.getAcceptMergeRequestOnSuccess()) {
            try {
                GitlabProject project = new GitlabProject();
                project.setId(projectId);
                GitlabAPI client = getClient(build);
                if (client == null) {
                    listener.getLogger().println("No GitLab connection configured");
                } else {
                    client.acceptMergeRequest(project, mergeRequestId, "Merge Request accepted by jenkins build success");
                }
            } catch (Throwable e) {
                listener.getLogger().println("Failed to accept merge request.");
            }
        }
    }

    private void addNoteOnMergeRequestIfNecessary(AbstractBuild<?, ?> build, GitLabPushTrigger trigger, TaskListener listener, Integer projectId, Integer mergeRequestId,
                                                  String projectName, int buildNumber, String buildUrl, String resultIcon, String statusDescription) {
        if (trigger.getAddNoteOnMergeRequest()) {
            String message = MessageFormat.format("{0} Jenkins Build {1}\n\nResults available at: [Jenkins [{2} #{3}]]({4})", resultIcon,
                    statusDescription, projectName, buildNumber, buildUrl);
            try {
                GitlabMergeRequest mergeRequest = new GitlabMergeRequest();
                mergeRequest.setProjectId(projectId);
                mergeRequest.setId(mergeRequestId);
                GitlabAPI client = getClient(build);
                if (client == null) {
                    listener.getLogger().println("No GitLab connection configured");
                } else {
                    client.createNote(mergeRequest, message);
                }
            } catch (IOException e) {
                listener.getLogger().println("Failed to accept merge request.");
            }
        }
    }

    private String getResultIcon(GitLabPushTrigger trigger, Result result) {
        if (result == Result.SUCCESS) {
            return trigger.getAddVoteOnMergeRequest() ? ":+1:" : ":white_check_mark:";
        } else {
            return trigger.getAddVoteOnMergeRequest() ? ":-1:" : ":anguished:";
        }
    }

    private GitlabAPI getClient(AbstractBuild<?, ?> run) {
        GitLabConnectionProperty connectionProperty = ((AbstractBuild<?, ?>) run).getProject().getProperty(GitLabConnectionProperty.class);
        if (connectionProperty != null) {
            return connectionProperty.getOldClient();
        }
        return null;
    }

}
