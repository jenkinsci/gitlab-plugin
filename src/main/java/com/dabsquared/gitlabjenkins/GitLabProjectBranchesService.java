package com.dabsquared.gitlabjenkins;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Branch;
import com.dabsquared.gitlabjenkins.util.LoggerUtil;
import com.dabsquared.gitlabjenkins.util.ProjectIdUtil;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class GitLabProjectBranchesService {

    private static final Logger LOGGER = Logger.getLogger(GitLabProjectBranchesService.class.getName());

    private final Cache<String, List<String>> projectBranchCache;

    private static transient GitLabProjectBranchesService gitLabProjectBranchesService;

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

    public List<String> getBranches(GitLabApi client, String sourceRepositoryString) {
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
        private final GitLabApi client;
        private final String sourceRepository;

        private BranchNamesLoader(GitLabApi client, String sourceRepository) {
            this.client = client;
            this.sourceRepository = sourceRepository;
        }

        @Override
        public List<String> call() throws Exception {
            List<String> result = new ArrayList<>();
            String projectId = ProjectIdUtil.retrieveProjectId(sourceRepository);
            for (Branch branch : client.getBranches(projectId)) {
                Optional<String> name = branch.optName();
                if (name.isPresent()) {
                    result.add(name.get());
                }
            }
            LOGGER.log(Level.FINEST, "found these branches for repo {0} : {1}", LoggerUtil.toArray(sourceRepository, result));
            return result;
        }
    }
}
