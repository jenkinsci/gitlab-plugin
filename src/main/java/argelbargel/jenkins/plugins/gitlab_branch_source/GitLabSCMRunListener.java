package argelbargel.jenkins.plugins.gitlab_branch_source;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import java.io.IOException;

@SuppressWarnings("unused")
@Extension
public class GitLabSCMRunListener extends RunListener<Run<?, ?>> {
    @Override
    public void onStarted(Run<?, ?> build, TaskListener listener) {
        GitLabSCMCauseAction action = build.getAction(GitLabSCMCauseAction.class);
        if (action != null && action.updateBuildDescription()) {
            onStarted(action.findCause(GitLabWebHookCause.class), build, listener);
        }
    }

    private void onStarted(GitLabWebHookCause cause, Run<?, ?> build, TaskListener listener) {
        if (cause != null && !cause.getShortDescription().isEmpty()) {
            try {
                build.setDescription(cause.getShortDescription());
            } catch (IOException e) {
                listener.getLogger().println("Failed to set build description");
            }
        }
    }
}
