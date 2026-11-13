package com.dabsquared.gitlabjenkins.util;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.Repository;
import hudson.model.Job;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.triggers.SCMTriggerItem;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

/**
 * GitLab sends the repository URL in several forms: the legacy webhook {@code repository} object
 * (generic/deprecated {@code url}, {@code git_ssh_url}, {@code git_http_url}) and the richer
 * {@code project} object (also {@code ssh_url}/{@code http_url}). The git plugin only triggers a
 * build from a {@code RevisionParameterAction} if its URL is an exact match of one of the job's
 * configured remote URLs, so a job cloning over HTTPS never matched a webhook that only carried
 * the SSH URL (and vice versa). This picks whichever known form actually matches a configured
 * remote.
 *
 * @see <a href="https://github.com/jenkinsci/gitlab-plugin/issues/477">jenkinsci/gitlab-plugin#477</a>
 */
public final class RepositoryUrlUtil {

    private static final Logger LOGGER = Logger.getLogger(RepositoryUrlUtil.class.getName());

    private RepositoryUrlUtil() {}

    public static GitSCM findGitSCM(Job<?, ?> job) {
        SCMTriggerItem item = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(job);
        if (item != null) {
            for (SCM scm : item.getSCMs()) {
                if (scm instanceof GitSCM gitSCM) {
                    return gitSCM;
                }
            }
        }
        return null;
    }

    public static URIish resolveUrIish(Repository repository, GitSCM gitSCM) {
        return resolveUrIish(repository, gitSCM == null ? null : gitSCM.getRepositories());
    }

    static URIish resolveUrIish(Repository repository, List<RemoteConfig> remoteConfigs) {
        if (repository == null) {
            return null;
        }
        List<String> candidates = new ArrayList<>();
        addIfNotBlank(candidates, repository.getUrl());
        addIfNotBlank(candidates, repository.getGitHttpUrl());
        addIfNotBlank(candidates, repository.getGitSshUrl());
        return resolveUrIish(candidates, remoteConfigs);
    }

    /**
     * Same as {@link #resolveUrIish(Repository, GitSCM)}, but sourced from a payload {@code project}
     * object. Unlike the legacy {@code repository} field, {@code project} is reliably populated with
     * both SSH and HTTP clone URLs on merge request, note and pipeline webhook events.
     */
    public static URIish resolveUrIish(Project project, GitSCM gitSCM) {
        return resolveUrIish(project, gitSCM == null ? null : gitSCM.getRepositories());
    }

    static URIish resolveUrIish(Project project, List<RemoteConfig> remoteConfigs) {
        if (project == null) {
            return null;
        }
        List<String> candidates = new ArrayList<>();
        addIfNotBlank(candidates, project.getUrl());
        addIfNotBlank(candidates, project.getHttpUrl());
        addIfNotBlank(candidates, project.getGitHttpUrl());
        addIfNotBlank(candidates, project.getSshUrl());
        addIfNotBlank(candidates, project.getGitSshUrl());
        return resolveUrIish(candidates, remoteConfigs);
    }

    private static URIish resolveUrIish(List<String> candidates, List<RemoteConfig> remoteConfigs) {
        if (remoteConfigs != null) {
            for (String candidate : candidates) {
                URIish candidateUri = parse(candidate);
                if (candidateUri != null && matchesConfiguredRemote(candidateUri, remoteConfigs)) {
                    return candidateUri;
                }
            }
        }

        return candidates.isEmpty() ? null : parse(candidates.get(0));
    }

    private static boolean matchesConfiguredRemote(URIish candidate, List<RemoteConfig> remoteConfigs) {
        for (RemoteConfig remoteConfig : remoteConfigs) {
            for (URIish remoteUri : remoteConfig.getURIs()) {
                if (remoteUri.equals(candidate)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void addIfNotBlank(List<String> list, String value) {
        if (StringUtils.isNotBlank(value)) {
            list.add(value);
        }
    }

    private static URIish parse(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        try {
            return new URIish(url);
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "could not parse URL {0}", url);
            return null;
        }
    }
}
