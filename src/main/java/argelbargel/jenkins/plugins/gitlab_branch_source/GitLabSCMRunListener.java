package argelbargel.jenkins.plugins.gitlab_branch_source;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;

@SuppressWarnings("unused")
@Extension
public class GitLabSCMRunListener extends RunListener<Run<?, ?>> {
    @Override
    public void onStarted(Run<?, ?> build, TaskListener listener) {
        GitLabSCMCauseAction action = build.getAction(GitLabSCMCauseAction.class);
        if (action != null && action.updateBuildDescription()) {
            onStarted(action.getDescription(), build, listener);
        }
    }

    private void onStarted(String description, Run<?, ?> build, TaskListener listener) {
        if (!StringUtils.isBlank(description)) {
            try {
                build.setDescription(description);
            } catch (IOException e) {
                listener.getLogger().println("Failed to set build description");
            }
        }
    }
}
