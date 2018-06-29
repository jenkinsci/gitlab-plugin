package com.dabsquared.gitlabjenkins.service;


import com.dabsquared.gitlabjenkins.gitlab.api.model.Label;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.dabsquared.gitlabjenkins.gitlab.api.model.builder.generated.LabelBuilder.label;
import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class GitLabProjectLabelsServiceTest {

    private final static List<String> LABELS_PROJECT_B = asList("label1", "label2", "label3");

    private GitLabProjectLabelsService labelsService;

    private GitLabClientStub clientStub;

    @Before
    public void setUp() throws IOException {
        clientStub = new GitLabClientStub();
        clientStub.addLabels("groupOne/A", convert(asList("label1", "label2")));
        clientStub.addLabels("groupOne/B", convert(LABELS_PROJECT_B));

        // never expire cache for tests
        labelsService = new GitLabProjectLabelsService();
    }

    @Test
    public void shouldReturnLabelsFromGitlabApi() {
        // when
        List<String> actualLabels = labelsService.getLabels(clientStub, "git@git.example.com:groupOne/B.git");

        // then
        assertThat(actualLabels, is(LABELS_PROJECT_B));
    }

    @Test
    public void shouldNotMakeUnnecessaryCallsToGitlabApiGetLabels() {
        // when
        labelsService.getLabels(clientStub, "git@git.example.com:groupOne/A.git");

        // then
        assertEquals(1, clientStub.calls("groupOne/A", Label.class));
        assertEquals(0, clientStub.calls("groupOne/B", Label.class));
    }

    private List<Label> convert(List<String> labels) {
        ArrayList<Label> result = new ArrayList<>();
        for (String label : labels) {
            result.add(label().withName(label).build());
        }
        return result;
    }
}
