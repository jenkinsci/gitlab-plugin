package com.dabsquared.gitlabjenkins.gitlab.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dabsquared.gitlabjenkins.gitlab.api.impl.AutodetectGitLabClientBuilder;
import com.dabsquared.gitlabjenkins.gitlab.api.impl.V3GitLabClientBuilder;
import com.dabsquared.gitlabjenkins.gitlab.api.impl.V4GitLabClientBuilder;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class GitLabClientBuilderTest {

    private JenkinsRule jenkins;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @Test
    void getAllGitLabClientBuilders_list_is_sorted_by_ordinal() {
        List<GitLabClientBuilder> builders = GitLabClientBuilder.getAllGitLabClientBuilders();
        assertThat(builders.get(0), instanceOf(AutodetectGitLabClientBuilder.class));
        assertThat(builders.get(1), instanceOf(V4GitLabClientBuilder.class));
        assertThat(builders.get(2), instanceOf(V3GitLabClientBuilder.class));
    }

    @Test
    void getGitLabClientBuilderById_success() {
        assertThat(
                GitLabClientBuilder.getGitLabClientBuilderById(new V3GitLabClientBuilder().id()),
                instanceOf(V3GitLabClientBuilder.class));
    }

    @Test
    void getGitLabClientBuilderById_no_match() {
        assertThrows(NoSuchElementException.class, () -> GitLabClientBuilder.getGitLabClientBuilderById("unknown"));
    }
}
