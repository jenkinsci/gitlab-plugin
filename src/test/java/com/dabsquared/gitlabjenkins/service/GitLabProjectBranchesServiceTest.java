package com.dabsquared.gitlabjenkins.service;

import static com.dabsquared.gitlabjenkins.gitlab.api.model.builder.generated.BranchBuilder.branch;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dabsquared.gitlabjenkins.gitlab.api.model.Branch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitLabProjectBranchesServiceTest {
    private static final List<String> BRANCH_NAMES_PROJECT_B = Arrays.asList("master", "B-branch-1", "B-branch-2");

    private GitLabProjectBranchesService branchesService;

    private GitLabClientStub clientStub;

    @BeforeEach
    void setUp() {
        clientStub = new GitLabClientStub();
        clientStub.addBranches("groupOne/A", convert(Arrays.asList("master", "A-branch-1")));
        clientStub.addBranches("groupOne/B", convert(BRANCH_NAMES_PROJECT_B));

        // never expire cache for tests
        branchesService = new GitLabProjectBranchesService();
    }

    @Test
    void shouldReturnBranchNamesFromGitlabApi() {
        // when
        List<String> actualBranchNames = branchesService.getBranches(clientStub, "git@git.example.com:groupOne/B.git");

        // then
        assertThat(actualBranchNames, is(BRANCH_NAMES_PROJECT_B));
    }

    @Test
    void shouldNotMakeUnnecessaryCallsToGitlabApiGetBranches() {
        // when
        branchesService.getBranches(clientStub, "git@git.example.com:groupOne/A.git");

        // then
        assertEquals(1, clientStub.calls("groupOne/A", Branch.class));
        assertEquals(0, clientStub.calls("groupOne/B", Branch.class));
    }

    private List<Branch> convert(List<String> branchNames) {
        ArrayList<Branch> result = new ArrayList<>();
        for (String branchName : branchNames) {
            result.add(branch().withName(branchName).build());
        }
        return result;
    }
}
