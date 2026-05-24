package com.dabsquared.gitlabjenkins.service;

import static com.dabsquared.gitlabjenkins.gitlab.api.model.builder.generated.LabelBuilder.label;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dabsquared.gitlabjenkins.gitlab.api.model.Label;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitLabProjectLabelsServiceTest {

    private static final List<String> LABELS_PROJECT_B = Arrays.asList("label1", "label2", "label3");

    private GitLabProjectLabelsService labelsService;

    private GitLabClientStub clientStub;

    @BeforeEach
    void setUp() {
        clientStub = new GitLabClientStub();
        clientStub.addLabels("groupOne/A", convert(Arrays.asList("label1", "label2")));
        clientStub.addLabels("groupOne/B", convert(LABELS_PROJECT_B));

        // never expire cache for tests
        labelsService = new GitLabProjectLabelsService();
    }

    @Test
    void shouldReturnLabelsFromGitlabApi() {
        // when
        List<String> actualLabels = labelsService.getLabels(clientStub, "git@git.example.com:groupOne/B.git");

        // then
        assertThat(actualLabels, is(LABELS_PROJECT_B));
    }

    @Test
    void shouldNotMakeUnnecessaryCallsToGitlabApiGetLabels() {
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
