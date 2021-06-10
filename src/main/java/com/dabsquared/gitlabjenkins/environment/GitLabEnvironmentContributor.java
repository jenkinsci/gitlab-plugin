package com.dabsquared.gitlabjenkins.environment;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.util.CauseUtil;
import hudson.EnvVars;
import hudson.Extension;
import hudson.matrix.MatrixRun;
import hudson.matrix.MatrixBuild;
import hudson.model.Cause;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

/**
 * @author Robin Müller
 */
@Extension
public class GitLabEnvironmentContributor extends EnvironmentContributor {
    @Override
    public void buildEnvironmentFor(@Nonnull Run r, @Nonnull EnvVars envs, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        List<Cause> causes = null;
        if (r instanceof MatrixRun) {
            MatrixBuild parent = ((MatrixRun)r).getParentBuild();
            if (parent != null) {
                causes = parent.getCauses();
            }
        } else {
            causes = r.getCauses();
        }

        GitLabWebHookCause cause = CauseUtil.findCauseFromUpstreamCauses(causes, GitLabWebHookCause.class);
        if (cause != null) {
            envs.overrideAll(cause.getData().getBuildVariables());
        }
    }
}
