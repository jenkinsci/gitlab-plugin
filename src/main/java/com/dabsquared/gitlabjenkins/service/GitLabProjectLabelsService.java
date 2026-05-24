package com.dabsquared.gitlabjenkins.service;

import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Label;
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

    public List<String> getLabels(GitLabClient client, String sourceRepositoryString) {
        synchronized (projectLabelsCache) {
            return projectLabelsCache.get(sourceRepositoryString, new LabelNamesLoader(client));
        }
    }

    public static class LabelLoadingException extends RuntimeException {
        LabelLoadingException(Throwable cause) {
            super(cause);
        }
    }

    private static class LabelNamesLoader implements Function<String, List<String>> {
        private final GitLabClient client;

        private LabelNamesLoader(GitLabClient client) {
            this.client = client;
        }

        @Override
        public List<String> apply(String sourceRepository) {
            List<String> result = new ArrayList<>();
            String projectId;
            try {
                projectId = ProjectIdUtil.retrieveProjectId(client, sourceRepository);
            } catch (ProjectIdUtil.ProjectIdResolutionException e) {
                throw new LabelLoadingException(e);
            }
            for (Label label : client.getLabels(projectId)) {
                result.add(label.getName());
            }
            LOGGER.log(
                    Level.FINEST,
                    "found these labels for repo {0} : {1}",
                    LoggerUtil.toArray(sourceRepository, result));
            return result;
        }
    }
}
