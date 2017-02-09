package argelbargel.jenkins.plugins.gitlab_branch_source;


import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPIException;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabProject;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.SubmoduleConfig;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.browser.GitLab;
import hudson.plugins.git.extensions.GitSCMExtension;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabAPI;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;


abstract class GitLabSCMHeadImpl extends GitLabSCMHead {
    private final String pronoun;
    private final GitLabSCMRefSpec ref;
    private transient Map<Integer, GitLabProject> projectCache;

    GitLabSCMHeadImpl(@Nonnull String name, @Nonnull String pronoun, @Nonnull GitLabSCMRefSpec ref) {
        super(ref.name(name));
        this.pronoun = pronoun;
        this.ref = ref;
    }

    @Override
    @CheckForNull
    public final String getPronoun() {
        return pronoun;
    }

    @Nonnull
    @Override
    final GitLabSCMRefSpec getRefSpec() {
        return ref;
    }

    @Nonnull
    final GitSCM createSCM(GitLabSCMSource source) {
        try {
            return new GitSCM(getRemotes(source), getBranchSpecs(),
                    false, Collections.<SubmoduleConfig>emptyList(),
                    getBrowser(source.getProjectId(), source), null, getExtensions(source));
        } catch (Exception e) {
            throw new RuntimeException("error creating scm for source + " + source.getId(), e);
        }
    }

    List<UserRemoteConfig> getRemotes(@Nonnull GitLabSCMSource source) throws GitLabAPIException {
        return singletonList(
                new UserRemoteConfig(
                        getProject(source.getProjectId(), source).getRemote(source),
                        "origin", getRefSpec().refSpec().toString(),
                        source.getCredentialsId()));
    }

    List<BranchSpec> getBranchSpecs() {
        return singletonList(new BranchSpec(getName()));
    }

    List<GitSCMExtension> getExtensions(GitLabSCMSource source) {
        return emptyList();
    }

    final GitLabProject getProject(int projectId, GitLabSCMSource source) throws GitLabAPIException {
        if (projectCache == null) {
            projectCache = new HashMap<>(1, 1.0f);
        }

        if (!projectCache.containsKey(projectId)) {
            projectCache.put(projectId, gitLabAPI(source.getConnectionName()).getProject(projectId));
        }

        return projectCache.get(projectId);
    }

    // TODO: do we need this? Would prefer it to stay in GitLabSCMSource only
    private GitLab getBrowser(int projectId, @Nonnull GitLabSCMSource source) throws GitLabAPIException {
        return new GitLab(getProject(projectId, source).getWebUrl(), source.getGitLabVersion());
    }
}
