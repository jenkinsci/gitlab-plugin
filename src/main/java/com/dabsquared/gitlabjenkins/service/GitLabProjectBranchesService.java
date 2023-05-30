package com.dabsquared.gitlabjenkins.service;

import com.dabsquared.gitlabjenkins.util.LoggerUtil;
import com.dabsquared.gitlabjenkins.util.ProjectIdUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Branch;

public class GitLabProjectBranchesService {

    private static final Logger LOGGER = Logger.getLogger(GitLabProjectBranchesService.class.getName());

    private static transient GitLabProjectBranchesService gitLabProjectBranchesService;
    private final Cache<String, List<String>> projectBranchCache;

    GitLabProjectBranchesService() {
        this.projectBranchCache = Caffeine.<String, String>newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .build();
    }

    public static GitLabProjectBranchesService instance() {
        if (gitLabProjectBranchesService == null) {
            gitLabProjectBranchesService = new GitLabProjectBranchesService();
        }
        return gitLabProjectBranchesService;
    }

    public List<String> getBranches(GitLabApi gitLabApi, String sourceRepositoryString) {
        synchronized (projectBranchCache) {
            return projectBranchCache.get(sourceRepositoryString, new BranchNamesLoader(gitLabApi));
        }
    }

    public static class BranchLoadingException extends RuntimeException {
        BranchLoadingException(Throwable cause) {
            super(cause);
        }
    }

    private static class BranchNamesLoader implements Function<String, List<String>> {
        private final GitLabApi gitLabApi;

        private BranchNamesLoader(GitLabApi gitLabApi) {
            this.gitLabApi = gitLabApi;
        }

        @Override
        public List<String> apply(String sourceRepository) {
            List<String> result = new ArrayList<>();
            String projectId;
            try {
                projectId = ProjectIdUtil.retrieveProjectId(gitLabApi, sourceRepository);
            } catch (ProjectIdUtil.ProjectIdResolutionException e) {
                throw new BranchLoadingException(e);
            }
            try {
                for (Branch branch : gitLabApi.getRepositoryApi().getBranches(projectId)) {
                    result.add(branch.getName());
                }
            } catch (GitLabApiException e) {
                LOGGER.log(Level.SEVERE, "failed to load branches from repository " + e.getMessage());
            }
            LOGGER.log(
                    Level.FINEST,
                    "found these branches for repo {0} : {1}",
                    LoggerUtil.toArray(sourceRepository, result));
            return result;
        }
    }
}
