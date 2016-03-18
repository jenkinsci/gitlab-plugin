package com.dabsquared.gitlabjenkins.listener;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.api.model.WebHook;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.model.Run;
import hudson.model.listeners.RunListener;

import java.io.IOException;
import java.util.Map;

/**
 * @author Robin Müller
 */
@Extension
public class GitLabEnvironmentRunListener extends RunListener<AbstractBuild<?, ?>> {
    @Override
    public Environment setUpEnvironment(final AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, Run.RunnerAbortedException {
        return new Environment() {
            @Override
            @SuppressWarnings("unchecked")
            public void buildEnvVars(Map<String, String> env) {
                GitLabWebHookCause<WebHook> cause = (GitLabWebHookCause<WebHook>) build.getCause(GitLabWebHookCause.class);
                if (cause != null) {
                    env.putAll(cause.getBuildVariables());
                }
            }
        };
    }
}
