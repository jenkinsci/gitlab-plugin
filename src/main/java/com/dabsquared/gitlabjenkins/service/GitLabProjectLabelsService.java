package com.dabsquared.gitlabjenkins.service;


import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Label;
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


public class GitLabProjectLabelsService {

    private static final Logger LOGGER = Logger.getLogger(GitLabProjectLabelsService.class.getName());

    private static transient GitLabProjectLabelsService instance;
    private final Cache<String, List<String>> projectLabelsCache;

    GitLabProjectLabelsService() {
        this.projectLabelsCache = CacheBuilder.<String, String>newBuilder()
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
            try {
                return projectLabelsCache.get(sourceRepositoryString, new LabelNamesLoader(client, sourceRepositoryString));
            } catch (ExecutionException e) {
                throw new LabelLoadingException(e);
            }
        }
    }

    public static class LabelLoadingException extends RuntimeException {
        LabelLoadingException(Throwable cause) {
            super(cause);
        }
    }

    private static class LabelNamesLoader implements Callable<List<String>> {
        private final GitLabClient client;
        private final String sourceRepository;

        private LabelNamesLoader(GitLabClient client, String sourceRepository) {
            this.client = client;
            this.sourceRepository = sourceRepository;
        }

        @Override
        public List<String> call() throws Exception {
            List<String> result = new ArrayList<>();
            String projectId = ProjectIdUtil.retrieveProjectId(client, sourceRepository);
            for (Label label : client.getLabels(projectId)) {
                result.add(label.getName());
            }
            LOGGER.log(Level.FINEST, "found these labels for repo {0} : {1}", LoggerUtil.toArray(sourceRepository, result));
            return result;
        }
    }
}
