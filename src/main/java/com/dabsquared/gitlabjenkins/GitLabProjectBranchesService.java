package com.dabsquared.gitlabjenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabProject;

public class GitLabProjectBranchesService {

    private static final Logger LOGGER = Logger.getLogger(GitLabProjectBranchesService.class.getName());

    /**
     * A map of git projects' branches; this is cached for
     * BRANCH_CACHE_TIME_IN_MILLISECONDS ms
     */
    private final Map<String, BranchListEntry> projectBranchCache = new HashMap<String, BranchListEntry>();

    /**
     * length of time a git project's branch list is kept in the
     * projectBranchCache for a particular source Repository
     */
    protected static final long BRANCH_CACHE_TIME_IN_MILLISECONDS = 5000;

    /**
     * a map of git projects; this is cached for
     * PROJECT_LIST_CACHE_TIME_IN_MILLISECONDS ms
     */
    private HashMap<String, GitlabProject> projectMapCache = new HashMap<String, GitlabProject>();

    /**
     * length of time the list of git project is kept without being refreshed
     * the map is also refreshed when a key hasnt been found, so we can leave
     * the cache time high e.g. 1 day:
     */
    protected static final long PROJECT_MAP_CACHE_TIME_IN_MILLISECONDS = 24 * 3600 * 1000;

    /**
     * time (epoch) the project cache will have expired
     */
    private long projectCacheExpiry;

    private final GitlabAPI gitlabAPI;

    private final TimeUtility timeUtility;

    protected GitLabProjectBranchesService(GitlabAPI gitlabAPI, TimeUtility timeUtility) {
        this.gitlabAPI = gitlabAPI;
        this.timeUtility = timeUtility;
    }

    public List<String> getBranches(String sourceRepositoryString) throws IOException {

        synchronized (projectBranchCache) {
            BranchListEntry branchListEntry = projectBranchCache.get(sourceRepositoryString);
            if (branchListEntry != null && !branchListEntry.hasExpired()) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST, "found branches in cache for {0}", sourceRepositoryString);
                }
                return branchListEntry.branchNames;
            }

            final List<String> branchNames = new ArrayList<String>();

            try {
                GitlabProject gitlabProject = findGitlabProjectForRepositoryUrl(sourceRepositoryString);
                if (gitlabProject != null) {
                    final List<GitlabBranch> branches = gitlabAPI.getBranches(gitlabProject);
                    for (final GitlabBranch branch : branches) {
                        branchNames.add(branch.getName());
                    }
                    projectBranchCache.put(sourceRepositoryString, new BranchListEntry(branchNames));

                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "found these branches for repo {0} : {1}",
                                new Object[] { sourceRepositoryString, branchNames.toString() });
                    }
                }
            } catch (final Error error) {
                /* WTF WTF WTF */
                final Throwable cause = error.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else {
                    throw error;
                }
            }
            return branchNames;
        }
    }

    public GitlabProject findGitlabProjectForRepositoryUrl(String sourceRepositoryString) throws IOException {
        synchronized (projectMapCache) {
            String repositoryUrl = sourceRepositoryString.toLowerCase();
            if (projectCacheExpiry < timeUtility.getCurrentTimeInMillis()
                    || !projectMapCache.containsKey(repositoryUrl)) {

                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINEST,
                            "refreshing repo map for {0} because expired : {1} or missing Key {2} expiry:{3} TS:{4}",
                            new Object[] { sourceRepositoryString,
                                    (Boolean) (projectCacheExpiry < timeUtility.getCurrentTimeInMillis()),
                                    (Boolean) projectMapCache.containsKey(repositoryUrl), projectCacheExpiry,
                                    timeUtility.getCurrentTimeInMillis() });
                }
                refreshGitLabProjectMap();
            }
            return projectMapCache.get(repositoryUrl);
        }
    }

    public Map<String, GitlabProject> refreshGitLabProjectMap() throws IOException {
        synchronized (projectMapCache) {
            try {
                projectMapCache.clear();
                List<GitlabProject> projects = gitlabAPI.getProjects();
                for (GitlabProject gitlabProject : projects) {
                    projectMapCache.put(gitlabProject.getSshUrl().toLowerCase(), gitlabProject);
                    projectMapCache.put(gitlabProject.getHttpUrl().toLowerCase(), gitlabProject);
                }
                projectCacheExpiry = timeUtility.getCurrentTimeInMillis() + PROJECT_MAP_CACHE_TIME_IN_MILLISECONDS;
            } catch (final Error error) {
                final Throwable cause = error.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else {
                    throw error;
                }
            }
            return projectMapCache;
        }
    }

    public class BranchListEntry {
        long expireTimestamp;
        List<String> branchNames;

        public BranchListEntry(List<String> branchNames) {
            this.branchNames = branchNames;
            this.expireTimestamp = timeUtility.getCurrentTimeInMillis() + BRANCH_CACHE_TIME_IN_MILLISECONDS;
        }

        boolean hasExpired() {
            return expireTimestamp < timeUtility.getCurrentTimeInMillis();
        }
    }

    public static class TimeUtility {
        public long getCurrentTimeInMillis() {
            return System.currentTimeMillis();
        }
    }

}
