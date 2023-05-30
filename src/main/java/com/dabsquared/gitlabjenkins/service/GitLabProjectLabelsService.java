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
import org.gitlab4j.api.models.Label;

public class GitLabProjectLabelsService {

    private static final Logger LOGGER = Logger.getLogger(GitLabProjectLabelsService.class.getName());

    private static transient GitLabProjectLabelsService instance;
    private final Cache<String, List<String>> projectLabelsCache;

    GitLabProjectLabelsService() {
        this.projectLabelsCache = Caffeine.<String, String>newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .build();
    }

    public static GitLabProjectLabelsService instance() {
        if (instance == null) {
            instance = new GitLabProjectLabelsService();
        }
        return instance;
    }

    public List<String> getLabels(GitLabApi gitLabApi, String sourceRepositoryString) {
        synchronized (projectLabelsCache) {
            return projectLabelsCache.get(sourceRepositoryString, new LabelNamesLoader(gitLabApi));
        }
    }

    public static class LabelLoadingException extends RuntimeException {
        LabelLoadingException(Throwable cause) {
            super(cause);
        }
    }

    private static class LabelNamesLoader implements Function<String, List<String>> {
        private final GitLabApi gitLabApi;

        private LabelNamesLoader(GitLabApi gitLabApi) {
            this.gitLabApi = gitLabApi;
        }

        @Override
        public List<String> apply(String sourceRepository) {
            List<String> result = new ArrayList<>();
            String projectId;
            try {
                projectId = ProjectIdUtil.retrieveProjectId(gitLabApi, sourceRepository);
            } catch (ProjectIdUtil.ProjectIdResolutionException e) {
                throw new LabelLoadingException(e);
            }
            try {
                for (Label label : gitLabApi.getLabelsApi().getLabels(projectId)) {
                    result.add(label.getName());
                }
            } catch (GitLabApiException e) {
                LOGGER.log(Level.SEVERE, "failed to load labels for repo " + e.getMessage());
            }
            LOGGER.log(
                    Level.FINEST,
                    "found these labels for repo {0} : {1}",
                    LoggerUtil.toArray(sourceRepository, result));
            return result;
        }
    }
}
