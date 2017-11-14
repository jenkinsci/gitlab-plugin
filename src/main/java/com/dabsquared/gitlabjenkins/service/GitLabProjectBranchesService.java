package com.dabsquared.gitlabjenkins.service;


import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Branch;
import com.dabsquared.gitlabjenkins.util.LoggerUtil;
import com.dabsquared.gitlabjenkins.util.ProjectIdUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class GitLabProjectBranchesService {

    private static final Logger LOGGER = Logger.getLogger(GitLabProjectBranchesService.class.getName());

    private static transient GitLabProjectBranchesService gitLabProjectBranchesService;
    private final Cache<String, List<String>> projectBranchCache;

    GitLabProjectBranchesService() {
        this.projectBranchCache = CacheBuilder.<String, String>newBuilder()
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
            try {
                return projectBranchCache.get(sourceRepositoryString, new BranchNamesLoader(client, sourceRepositoryString));
            } catch (ExecutionException e) {
                throw new BranchLoadingException(e);
            }
        }
    }

    public static class BranchLoadingException extends RuntimeException {
        BranchLoadingException(Throwable cause) {
            super(cause);
        }
    }

    private static class BranchNamesLoader implements Callable<List<String>> {
        private final GitLabClient client;
        private final String sourceRepository;

        private BranchNamesLoader(GitLabClient client, String sourceRepository) {
            this.client = client;
            this.sourceRepository = sourceRepository;
        }

        @Override
        public List<String> call() throws Exception {
            List<String> result = new ArrayList<>();
            String projectId = ProjectIdUtil.retrieveProjectId(client, sourceRepository);
            for (Branch branch : client.getBranches(projectId)) {
                result.add(branch.getName());
            }
            LOGGER.log(Level.FINEST, "found these branches for repo {0} : {1}", LoggerUtil.toArray(sourceRepository, result));
            return result;
        }
    }
}
