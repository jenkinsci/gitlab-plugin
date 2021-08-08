package com.dabsquared.gitlabjenkins.service;


import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Branch;
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

    public List<String> getBranches(GitLabClient client, String sourceRepositoryString) {
        synchronized (projectBranchCache) {
            return projectBranchCache.get(sourceRepositoryString, new BranchNamesLoader(client));
        }
    }

    public static class BranchLoadingException extends RuntimeException {
        BranchLoadingException(Throwable cause) {
            super(cause);
        }
    }

    private static class BranchNamesLoader implements Function<String, List<String>> {
        private final GitLabClient client;

        private BranchNamesLoader(GitLabClient client) {
            this.client = client;
        }

        @Override
        public List<String> apply(String sourceRepository) {
            List<String> result = new ArrayList<>();
            String projectId;
            try {
                projectId = ProjectIdUtil.retrieveProjectId(client, sourceRepository);
            } catch (ProjectIdUtil.ProjectIdResolutionException e) {
                throw new BranchLoadingException(e);
            }
            for (Branch branch : client.getBranches(projectId)) {
                result.add(branch.getName());
            }
            LOGGER.log(Level.FINEST, "found these branches for repo {0} : {1}", LoggerUtil.toArray(sourceRepository, result));
            return result;
        }
    }
}
