package com.dabsquared.gitlabjenkins.service;


import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Branch;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.dabsquared.gitlabjenkins.gitlab.api.model.builder.generated.BranchBuilder.branch;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GitLabProjectBranchesServiceTest {

    private final static List<String> BRANCH_NAMES_PROJECT_B = asList("master", "B-branch-1", "B-branch-2");

    private GitLabProjectBranchesService branchesService;

    @Mock
    private GitLabClient gitlabClient;

    @Before
    public void setUp() throws IOException {
        List<Branch> branchNamesProjectA = convert(asList("master", "A-branch-1"));

        // mock the gitlab factory
        when(gitlabClient.getBranches("groupOne/A")).thenReturn(branchNamesProjectA);
        when(gitlabClient.getBranches("groupOne/B")).thenReturn(convert(BRANCH_NAMES_PROJECT_B));

        // never expire cache for tests
        branchesService = new GitLabProjectBranchesService();
    }

    @Test
    public void shouldReturnBranchNamesFromGitlabApi() {
        // when
        List<String> actualBranchNames = branchesService.getBranches(gitlabClient, "git@git.example.com:groupOne/B.git");

        // then
        assertThat(actualBranchNames, is(BRANCH_NAMES_PROJECT_B));
    }

    @Test
    public void shouldNotMakeUnnecessaryCallsToGitlabApiGetBranches() {
        // when
        branchesService.getBranches(gitlabClient, "git@git.example.com:groupOne/A.git");

        // then
        verify(gitlabClient, times(1)).getBranches("groupOne/A");
        verify(gitlabClient, times(0)).getBranches("groupOne/B");
    }

    private List<Branch> convert(List<String> branchNames) {
        ArrayList<Branch> result = new ArrayList<>();
        for (String branchName : branchNames) {
            result.add(branch().withName(branchName).build());
        }
        return result;
    }
}
