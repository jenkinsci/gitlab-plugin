package com.dabsquared.gitlabjenkins.service;


import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Label;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.dabsquared.gitlabjenkins.gitlab.api.model.builder.generated.LabelBuilder.label;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GitLabProjectLabelsServiceTest {

    private final static List<String> LABELS_PROJECT_B = asList("label1", "label2", "label3");

    private GitLabProjectLabelsService labelsService;

    @Mock
    private GitLabClient gitlabClient;

    @Before
    public void setUp() throws IOException {
        List<Label> labelsProjectA = convert(asList("label1", "label2"));

        // mock the gitlab factory
        when(gitlabClient.getLabels("groupOne/A")).thenReturn(labelsProjectA);
        when(gitlabClient.getLabels("groupOne/B")).thenReturn(convert(LABELS_PROJECT_B));

        // never expire cache for tests
        labelsService = new GitLabProjectLabelsService();
    }

    @Test
    public void shouldReturnLabelsFromGitlabApi() {
        // when
        List<String> actualLabels = labelsService.getLabels(gitlabClient, "git@git.example.com:groupOne/B.git");

        // then
        assertThat(actualLabels, is(LABELS_PROJECT_B));
    }

    @Test
    public void shouldNotMakeUnnecessaryCallsToGitlabApiGetLabels() {
        // when
        labelsService.getLabels(gitlabClient, "git@git.example.com:groupOne/A.git");

        // then
        verify(gitlabClient, times(1)).getLabels("groupOne/A");
        verify(gitlabClient, times(0)).getLabels("groupOne/B");
    }

    private List<Label> convert(List<String> labels) {
        ArrayList<Label> result = new ArrayList<>();
        for (String label : labels) {
            result.add(label().withName(label).build());
        }
        return result;
    }
}
