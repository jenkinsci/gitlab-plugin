package argelbargel.jenkins.plugins.gitlab_branch_source;


import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;


@SuppressWarnings("unused")
@Extension
public class GitLabSCMRunListener extends RunListener<Run<?, ?>> {
    @Override
    public void onStarted(Run<?, ?> build, TaskListener listener) {
        if (build instanceof WorkflowRun) {
            attachListener((WorkflowRun) build);
        }

        GitLabSCMCauseAction causeAction = build.getParent().getAction(GitLabSCMCauseAction.class);
        GitLabSCMPublishAction publishAction = build.getParent().getAction(GitLabSCMPublishAction.class);
        if (causeAction != null && publishAction != null) {
            publishAction.updateBuildDescription(build, causeAction, listener);
            publishAction.publishPending(build, causeAction);
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

    private void attachListener(final WorkflowRun build) {
        build.getExecutionPromise().addListener(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        System.out.println(build.getExecution());
                                                    }
                                                },
                new Executor() {
                    @Override
                    public void execute(Runnable command) {
                        command.run();
                    }
                });
    }
}
