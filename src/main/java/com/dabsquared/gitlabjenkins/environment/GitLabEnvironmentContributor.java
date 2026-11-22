package com.dabsquared.gitlabjenkins.environment;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.EnvironmentContributor;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;

/**
 * @author Robin Müller
 */
@Extension
public class GitLabEnvironmentContributor extends EnvironmentContributor {
    @Override
    public void buildEnvironmentFor(@NonNull Run r, @NonNull EnvVars envs, @NonNull TaskListener listener)
            throws IOException, InterruptedException {
        GitLabWebHookCause cause = null;
        Job<?, ?> job = r.getParent();

        if (r instanceof MatrixRun run) {
            MatrixBuild parent = run.getParentBuild();
            if (parent != null) {
                cause = (GitLabWebHookCause) parent.getCause(GitLabWebHookCause.class);
                job = parent.getParent();
            }
        } else {
            cause = (GitLabWebHookCause) r.getCause(GitLabWebHookCause.class);
        }

        if (cause != null) {
            envs.overrideAll(cause.getData().getBuildVariables());

            GitLabPushTrigger trigger = GitLabPushTrigger.getFromJob(job);
            if (trigger != null && trigger.getExposeRawWebhookPayload() && cause.getRawPayload() != null) {
                envs.put("gitlabWebhookPayload", cause.getRawPayload());
            }
        }
    }
}
