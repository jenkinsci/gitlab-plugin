package com.dabsquared.gitlabjenkins.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

import com.dabsquared.gitlabjenkins.util.ProjectIdUtil;
import java.util.Arrays;
import java.util.List;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.LabelsApi;
import org.gitlab4j.api.models.Label;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GitLabProjectLabelsServiceTest {

    @Mock
    private GitLabApi gitLabApi;

    @Mock
    private LabelsApi labelsApi;

    private GitLabProjectLabelsService labelsService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        labelsService = GitLabProjectLabelsService.instance();
    }

    @Test
    public void shouldReturnLabelsFromGitlabApi() throws Exception {
        String sourceRepository = "git@git.example.com:groupOne/A.git";
        String projectId = "groupOne/A";
        List<Label> labels = Arrays.asList(new Label().withName("bug"), new Label().withName("feature"));
        when(ProjectIdUtil.retrieveProjectId(gitLabApi, sourceRepository)).thenReturn(projectId);
        when(gitLabApi.getLabelsApi()).thenReturn(labelsApi);
        when(labelsApi.getLabels(projectId)).thenReturn(labels);

        List<String> actualLabelNames = labelsService.getLabels(gitLabApi, sourceRepository);

        assertThat(actualLabelNames, is(Arrays.asList("bug", "feature")));
    }
}
