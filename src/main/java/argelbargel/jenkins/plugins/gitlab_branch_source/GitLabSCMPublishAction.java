package argelbargel.jenkins.plugins.gitlab_branch_source;


import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import hudson.model.InvisibleAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.Serializable;

import static com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState.canceled;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState.failed;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState.pending;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState.success;
import static hudson.model.Result.ABORTED;
import static hudson.model.Result.UNSTABLE;


class GitLabSCMPublishAction extends InvisibleAction implements Serializable {
    private final String publisherName;
    private final boolean markUnstableAsSuccess;
    private final boolean updateBuildDescription;

    GitLabSCMPublishAction(String publisherName, boolean markUnstableAsSuccess, boolean updateBuildDescription) {
        this.publisherName = publisherName;
        this.markUnstableAsSuccess = markUnstableAsSuccess;
        this.updateBuildDescription = updateBuildDescription;
    }

    void updateBuildDescription(Run<?, ?> build, GitLabSCMCauseAction action, TaskListener listener) {
        if (updateBuildDescription && !StringUtils.isBlank(action.getDescription())) {
            try {
                build.setDescription(action.getDescription());
            } catch (IOException e) {
                listener.getLogger().println("Failed to set build description");
            }
        }
    }

    void publishPending(Run<?, ?> build, GitLabSCMCauseAction cause, TaskListener listener) {
        publishBuildStatus(build, cause, pending, cause.getDescription(), listener);
    }

    void publishResult(Run<?, ?> build, GitLabSCMCauseAction cause, TaskListener listener) {
        Result buildResult = build.getResult();
        if ((buildResult == Result.SUCCESS) || ((buildResult == UNSTABLE) && markUnstableAsSuccess)) {
            publishBuildStatus(build, cause, success, "", listener);
        } else if (buildResult == ABORTED) {
            publishBuildStatus(build, cause, canceled, "", listener);
        } else {
            publishBuildStatus(build, cause, failed, "", listener);
        }
    }

    private void publishBuildStatus(Run<?, ?> build, GitLabSCMCauseAction cause, BuildState state, String description, TaskListener listener) {
        GitLabApi client = GitLabConnectionProperty.getClient(build);
        if (client != null) {
            try {
                client.changeBuildStatus(cause.getProjectId(), cause.getHash(), state, cause.getRef(), publisherName,
                        Jenkins.getInstance().getRootUrl() + build.getUrl() + build.getNumber(), description);
            } catch (Exception e) {
                listener.getLogger().format("Failed to set gitlab-build-status to pending: " + e.getMessage());
            }
        } else {
            listener.getLogger().format("cannot publish build-status pending as no gitlab-connection is configured!");
        }
    }
}
