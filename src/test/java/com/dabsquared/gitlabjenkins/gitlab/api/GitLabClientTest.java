package com.dabsquared.gitlabjenkins.gitlab.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dabsquared.gitlabjenkins.gitlab.api.model.Pipeline;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitLabClientTest {

    private GitLabClient client;

    @BeforeEach
    void setUp() {
        client = mock(GitLabClient.class);
        when(client.getPipelines(any(), any(), any())).thenCallRealMethod();
        when(client.getPipelines("project"))
                .thenReturn(List.of(
                        pipeline(1, "aaa", "push"),
                        pipeline(2, "aaa", "merge_request_event"),
                        pipeline(3, "bbb", "merge_request_event")));
    }

    @Test
    void defaultGetPipelinesFiltersBySha() {
        assertEquals(List.of(1, 2), ids(client.getPipelines("project", "aaa", null)));
    }

    @Test
    void defaultGetPipelinesFiltersByShaAndSource() {
        assertEquals(List.of(2), ids(client.getPipelines("project", "aaa", "merge_request_event")));
    }

    @Test
    void defaultGetPipelinesWithoutFiltersReturnsEverything() {
        assertEquals(List.of(1, 2, 3), ids(client.getPipelines("project", null, null)));
    }

    private static List<Integer> ids(List<Pipeline> pipelines) {
        return pipelines.stream().map(Pipeline::getId).toList();
    }

    private static Pipeline pipeline(int id, String sha, String source) {
        Pipeline pipeline = new Pipeline();
        pipeline.setId(id);
        pipeline.setSha(sha);
        pipeline.setSource(source);
        return pipeline;
    }
}
