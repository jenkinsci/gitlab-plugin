package com.dabsquared.gitlabjenkins.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

import com.dabsquared.gitlabjenkins.util.ProjectIdUtil;
import java.util.Arrays;
import java.util.List;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.RepositoryApi;
import org.gitlab4j.api.models.Branch;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GitLabProjectBranchesServiceTest {

    @Mock
    private GitLabApi gitLabApi;

    @Mock
    private RepositoryApi repositoryApi;

    private GitLabProjectBranchesService branchesService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        branchesService = GitLabProjectBranchesService.instance();
    }

    @Test
    public void shouldReturnBranchNamesFromGitlabApi() throws Exception {

        String sourceRepository = "git@git.example.com:groupOne/B.git";
        String projectId = "groupOne/B";
        List<Branch> branches = Arrays.asList(
                new Branch().withName("master"),
                new Branch().withName("B-branch-1"),
                new Branch().withName("B-branch-2"));

        when(ProjectIdUtil.retrieveProjectId(gitLabApi, sourceRepository)).thenReturn(projectId);
        when(gitLabApi.getRepositoryApi()).thenReturn(repositoryApi);
        when(repositoryApi.getBranches(projectId)).thenReturn(branches);
        List<String> actualBranchNames = branchesService.getBranches(gitLabApi, sourceRepository);
        assertThat(actualBranchNames, is(Arrays.asList("master", "B-branch-1", "B-branch-2")));
    }
}
