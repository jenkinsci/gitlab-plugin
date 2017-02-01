package argelbargel.jenkins.plugins.gitlab_branch_source.api;

import org.apache.commons.lang.StringUtils;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabProjectHook;
import org.gitlab.api.models.GitlabSystemHook;
import org.gitlab.api.models.GitlabTag;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

public final class GitLabAPI {
    private static final Logger LOGGER = Logger.getLogger(GitLabAPI.class.getName());

    private final GitlabAPI delegate;

    public GitLabAPI(GitlabAPI delegate) {
        this.delegate = delegate;
    }

    public GitLabVersion getVersion() throws GitLabAPIException {
        try {
            return delegate.retrieve().to("/version", GitLabVersion.class);
        } catch (IOException e) {
            throw new GitLabAPIException(e);
        }
    }

    public GitlabProject getProject(int id) throws GitLabAPIException {
        return getProject((Serializable) id);
    }

    public GitlabProject getProject(String name) throws GitLabAPIException {
        return getProject((Serializable) name);
    }

    private GitlabProject getProject(Serializable nameOrId) throws GitLabAPIException {
        try {
            return delegate.getProject(nameOrId);
        } catch (IOException e) {
            throw new GitLabAPIException(e);
        }
    }

    public List<GitlabBranch> getBranches(int id) throws GitLabAPIException {
        return getBranches((Serializable) id);
    }

    private List<GitlabBranch> getBranches(Serializable nameOrId) throws GitLabAPIException {
        try {
            return delegate.getBranches(nameOrId);
        } catch (IOException e) {
            throw new GitLabAPIException(e);
        }
    }

    public List<GitlabTag> getTags(int id) throws GitLabAPIException {
        return getTags((Serializable) id);
    }

    private List<GitlabTag> getTags(Serializable nameOrId) throws GitLabAPIException {
        try {
            return delegate.getTags(nameOrId);
        } catch (IOException e) {
            throw new GitLabAPIException(e);
        }
    }

    public List<GitLabMergeRequest> getMergeRequests(int id) {
        return getMergeRequests((Serializable) id);
    }

    private List<GitLabMergeRequest> getMergeRequests(Serializable nameOrId) {
        String tailUrl = "/projects/" + nameOrId + "/merge_requests?state=opened";
        return delegate.retrieve()
                .getAll(tailUrl, GitLabMergeRequest[].class);
    }


    public List<GitlabProject> findProjects(GitLabProjectSelector selector, GitLabProjectVisibility visibility, String searchPattern) {
        LOGGER.fine("finding projects for " + selector + ", " + visibility + ", " + searchPattern + "...");
        return delegate
                .retrieve()
                .getAll(projectUrl(selector, visibility, searchPattern), GitlabProject[].class);
    }

    public GitlabSystemHook registerSystemHook(URL url) throws GitLabAPIException {
        try {
            return registerSystemHook(url.toString());
        } catch (IOException e) {
            throw new GitLabAPIException(e);
        }
    }

    private GitlabSystemHook registerSystemHook(String url) throws IOException {
        LOGGER.fine("registering system-hook " + url + "...");
        for (GitlabSystemHook hook : delegate.getSystemHooks()) {
            if (hook.getUrl().equals(url)) {
                return hook;
            }
        }

        return delegate.dispatch()
                .with("url", url)
                .with("push_events", false)
                .to("/hooks", GitlabSystemHook.class);
    }

    public boolean unregisterSystemHook(URL url) throws GitLabAPIException {
        try {
            return unregisterSystemHook(url.toString());
        } catch (IOException e) {
            throw new GitLabAPIException(e);
        }
    }

    private boolean unregisterSystemHook(String url) throws IOException {
        LOGGER.finer("looking up system-hooks...");
        for (GitlabSystemHook hook : delegate.getSystemHooks()) {
            if (hook.getUrl().equals(url)) {
                LOGGER.fine("un-registering system-hook " + url + "...");
                delegate.deleteSystemHook(hook.getId());
                return true;
            }
        }
        return false;
    }

    public GitlabProjectHook registerProjectHook(URL url, int projectId) throws GitLabAPIException {
        try {
            return registerProjectHook(url.toString(), projectId);
        } catch (IOException e) {
            throw new GitLabAPIException(e);
        }
    }

    private GitlabProjectHook registerProjectHook(String url, int projectId) throws IOException {
        LOGGER.fine("registering project-hook for project " + projectId + ": " + url + "...");
        for (GitlabProjectHook hook : delegate.getProjectHooks(projectId)) {
            if (hook.getUrl().equals(url)) {
                return hook;
            }
        }

        return delegate.addProjectHook(projectId, url, true, false, true, true, false);
    }

    public boolean unregisterProjectHook(URL url, int projectId) throws GitLabAPIException {
        try {
            return unregisterProjectHook(url.toString(), projectId);
        } catch (IOException e) {
            throw new GitLabAPIException(e);
        }
    }

    private boolean unregisterProjectHook(String url, int projectId) throws IOException {
        LOGGER.finer("looking up project-hooks for project " + projectId + "...");
        for (GitlabProjectHook hook : delegate.getProjectHooks(projectId)) {
            if (hook.getUrl().equals(url)) {
                LOGGER.fine("un-registering project-hook for project " + projectId + ": " + url + "...");
                delegate.deleteProjectHook(hook);
                return true;
            }
        }

        return false;
    }

    private String projectUrl(GitLabProjectSelector selector, GitLabProjectVisibility visibility, String searchPattern) {
        StringBuilder urlBuilder = new StringBuilder(GitlabProject.URL)
                .append("/").append(selector.id());

        if (!GitLabProjectVisibility.ALL.equals(visibility)) {
            urlBuilder.append("?visibility=").append(visibility.id());
        }

        if (!StringUtils.isEmpty(searchPattern)) {
            urlBuilder.append(GitLabProjectVisibility.ALL.equals(visibility) ? "?" : "&").append("search=").append(searchPattern);
        }

        return urlBuilder.toString();
    }
}
