package com.dabsquared.gitlabjenkins.connection;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.credentialsbinding.BindingDescriptor;
import org.jenkinsci.plugins.credentialsbinding.MultiBinding;
import org.kohsuke.stapler.DataBoundConstructor;

public class GitLabApiTokenBinding extends MultiBinding<GitLabApiTokenImpl> {

    private final String variable;

    @DataBoundConstructor
    public GitLabApiTokenBinding(String credentialsId, String variable) {
        super(credentialsId);
        this.variable = variable;
    }

    @Override
    protected Class<GitLabApiTokenImpl> type() {
        return GitLabApiTokenImpl.class;
    }

    @Override
    public Set<String> variables() {
        return Collections.singleton(variable);
    }

    @Override
    public MultiEnvironment bind(
            @NonNull Run<?, ?> build,
            @Nullable FilePath workspace,
            @Nullable Launcher launcher,
            @NonNull TaskListener listener)
            throws IOException, InterruptedException {
        GitLabApiTokenImpl credentials = getCredentials(build);
        Map<String, String> values = new LinkedHashMap<>();
        values.put(variable, Secret.toString(credentials.getApiToken()));
        return new MultiEnvironment(values);
    }

    @Symbol("gitlabApiToken")
    @Extension
    public static class DescriptorImpl extends BindingDescriptor<GitLabApiTokenImpl> {

        @Override
        protected Class<GitLabApiTokenImpl> type() {
            return GitLabApiTokenImpl.class;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.GitLabApiToken_name();
        }

        @Override
        public boolean requiresWorkspace() {
            return false;
        }
    }
}
