package argelbargel.jenkins.plugins.gitlab_branch_source;


import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import hudson.model.InvisibleAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.Serializable;

import static com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState.canceled;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState.failed;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState.pending;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState.success;
import static hudson.model.Result.ABORTED;
import static hudson.model.Result.SUCCESS;
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

    void publishPending(Run<?, ?> build, GitLabSCMCauseAction cause) {
        publishBuildStatus(build, cause, pending, cause.getDescription());
    }

    void publishResult(Run<?, ?> build, GitLabSCMCauseAction cause) {
        Result buildResult = build.getResult();
        if ((buildResult == SUCCESS) || ((buildResult == UNSTABLE) && markUnstableAsSuccess)) {
            publishBuildStatus(build, cause, success, "");
        } else if (buildResult == ABORTED) {
            publishBuildStatus(build, cause, canceled, "");
        } else {
            publishBuildStatus(build, cause, failed, "");
        }
    }

    private void publishBuildStatus(Run<?, ?> build, GitLabSCMCauseAction cause, BuildState state, String description) {
        GitLabSCMBuildStatusPublisher.instance()
                .publish(build, publisherName, cause.getProjectId(), cause.getRef(), cause.getHash(), state, description);
    }
}
