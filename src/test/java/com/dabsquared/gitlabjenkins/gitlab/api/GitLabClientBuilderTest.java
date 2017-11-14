package com.dabsquared.gitlabjenkins.gitlab.api;


import com.dabsquared.gitlabjenkins.gitlab.api.impl.AutodetectGitLabClientBuilder;
import com.dabsquared.gitlabjenkins.gitlab.api.impl.V3GitLabClientBuilder;
import com.dabsquared.gitlabjenkins.gitlab.api.impl.V4GitLabClientBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.List;
import java.util.NoSuchElementException;

import static com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder.getAllGitLabClientBuilders;
import static com.dabsquared.gitlabjenkins.gitlab.api.GitLabClientBuilder.getGitLabClientBuilderById;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;


public class GitLabClientBuilderTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void getAllGitLabClientBuilders_list_is_sorted_by_ordinal() {
        List<GitLabClientBuilder> builders = getAllGitLabClientBuilders();
        assertThat(builders.get(0), instanceOf(AutodetectGitLabClientBuilder.class));
        assertThat(builders.get(1), instanceOf(V4GitLabClientBuilder.class));
        assertThat(builders.get(2), instanceOf(V3GitLabClientBuilder.class));
    }

    @Test
    public void getGitLabClientBuilderById_success() {
        assertThat(getGitLabClientBuilderById(new V3GitLabClientBuilder().id()), instanceOf(V3GitLabClientBuilder.class));
    }

    @Test(expected = NoSuchElementException.class)
    public void getGitLabClientBuilderById_no_match() {
        getGitLabClientBuilderById("unknown");
    }
}
