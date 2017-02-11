package argelbargel.jenkins.plugins.gitlab_branch_source;


import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import javax.annotation.Nonnull;


@SuppressWarnings("unused")
@Extension
public class GitLabSCMRunListener extends RunListener<Run<?, ?>> {
    @Override
    public void onStarted(Run<?, ?> build, TaskListener listener) {
        GitLabSCMCauseAction causeAction = build.getParent().getAction(GitLabSCMCauseAction.class);
        GitLabSCMPublishAction publishAction = build.getParent().getAction(GitLabSCMPublishAction.class);
        if (causeAction != null && publishAction != null) {
            publishAction.updateBuildDescription(build, causeAction, listener);
            publishAction.publishStarted(build, causeAction);
        }
    }

    @Override
    public void onCompleted(Run<?, ?> build, @Nonnull TaskListener listener) {
        GitLabSCMCauseAction causeAction = build.getParent().getAction(GitLabSCMCauseAction.class);
        GitLabSCMPublishAction publishAction = build.getParent().getAction(GitLabSCMPublishAction.class);
        if (causeAction != null && publishAction != null) {
            publishAction.publishResult(build, causeAction);
        }
    }
}
