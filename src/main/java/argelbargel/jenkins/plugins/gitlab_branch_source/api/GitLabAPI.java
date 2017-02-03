package argelbargel.jenkins.plugins.gitlab_branch_source.api;

import org.apache.commons.lang.StringUtils;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabProjectHook;
import org.gitlab.api.models.GitlabSystemHook;
import org.gitlab.api.models.GitlabTag;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

public final class GitLabAPI {
    private static final String PATH_SEP = "/";

    public static GitLabAPI connect(String url, String token) throws GitLabAPIException {
        try {
            return new GitLabAPI(GitlabAPI.connect(url, token));
        } catch (Exception e) {
            throw new GitLabAPIException(e);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(GitLabAPI.class.getName());

    private final GitlabAPI delegate;

    private GitLabAPI(GitlabAPI delegate) {
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
        } catch (FileNotFoundException e) {
            throw new NoSuchElementException("unknown project " + nameOrId);
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
        } catch (Exception e) {
            throw new GitLabAPIException(e);
        }
    }

    public GitlabBranch getBranch(int projectId, String branch) throws GitLabAPIException {
        try {
            String tailUrl = GitlabProject.URL + PATH_SEP + projectId + GitlabBranch.URL + PATH_SEP + URLEncoder.encode(branch, "UTF-8");
            return delegate.retrieve().to(tailUrl, GitlabBranch.class);
        } catch (FileNotFoundException e) {
            throw new NoSuchElementException("unknown branch " + branch);
        } catch (Exception e) {
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

    public GitlabTag getTag(int projectId, String tag) throws GitLabAPIException {
        try {
            String tailUrl = GitlabProject.URL + PATH_SEP + projectId + GitlabTag.URL + PATH_SEP + URLEncoder.encode(tag, "UTF-8");
            return delegate.retrieve().to(tailUrl, GitlabTag.class);
        } catch (FileNotFoundException e) {
            throw new NoSuchElementException("unknown tag " + tag);
        } catch (Exception e) {
            throw new GitLabAPIException(e);
        }
    }

    public List<GitLabMergeRequest> getMergeRequests(int id) throws GitLabAPIException {
        return getMergeRequests((Serializable) id);
    }

    private List<GitLabMergeRequest> getMergeRequests(Serializable nameOrId) throws GitLabAPIException {
        try {
            String tailUrl = "/projects/" + nameOrId + "/merge_requests?state=opened";
            return delegate.retrieve()
                    .getAll(tailUrl, GitLabMergeRequest[].class);
        } catch (Exception e) {
            throw new GitLabAPIException(e);
        }
    }


    public List<GitlabProject> findProjects(GitLabProjectSelector selector, GitLabProjectVisibility visibility, String searchPattern) throws GitLabAPIException {
        LOGGER.fine("finding projects for " + selector + ", " + visibility + ", " + searchPattern + "...");
        try {
            return delegate
                    .retrieve()
                    .getAll(projectUrl(selector, visibility, searchPattern), GitlabProject[].class);
        } catch (Exception e) {
            throw new GitLabAPIException(e);
        }
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
                String tailUrl = GitlabProject.URL + PATH_SEP + hook.getProjectId() + GitlabProjectHook.URL + PATH_SEP + hook.getId();
                delegate.retrieve().method("DELETE").to(tailUrl, GitlabProjectHook[].class);
                return true;
            }
        }

        return false;
    }

    private String projectUrl(GitLabProjectSelector selector, GitLabProjectVisibility visibility, String searchPattern) {
        StringBuilder urlBuilder = new StringBuilder(GitlabProject.URL)
                .append(PATH_SEP).append(selector.id());

        if (!GitLabProjectVisibility.ALL.equals(visibility)) {
            urlBuilder.append("?visibility=").append(visibility.id());
        }

        if (!StringUtils.isEmpty(searchPattern)) {
            urlBuilder.append(GitLabProjectVisibility.ALL.equals(visibility) ? "?" : "&").append("search=").append(searchPattern);
        }

        return urlBuilder.toString();
    }
}
