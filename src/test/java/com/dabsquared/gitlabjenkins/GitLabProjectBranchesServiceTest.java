package com.dabsquared.gitlabjenkins;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabNamespace;
import org.gitlab.api.models.GitlabProject;
import org.junit.Before;
import org.junit.Test;

import com.dabsquared.gitlabjenkins.GitLabProjectBranchesService.TimeUtility;

public class GitLabProjectBranchesServiceTest {

    private GitLabProjectBranchesService branchesService;

    private GitlabAPI gitlabApi;
    private TimeUtility timeUtility;

    private GitlabProject gitlabProjectA;
    private GitlabProject gitlabProjectB;

    private List<String> branchNamesProjectA;
    private List<String> branchNamesProjectB;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws IOException {

        // some test data
        gitlabProjectA = setupGitlabProject("groupOne", "A");
        gitlabProjectB = setupGitlabProject("groupOne", "B");

        branchNamesProjectA = asList("master", "A-branch-1");
        branchNamesProjectB = asList("master", "B-branch-1", "B-branch-2");

        // mock the gitlab factory
        mockGitlab(asList(gitlabProjectA, gitlabProjectB), asList(branchNamesProjectA, branchNamesProjectB));

        // never expire cache for tests
        timeUtility = mock(TimeUtility.class);
        when(timeUtility.getCurrentTimeInMillis()).thenReturn(1L);

        branchesService = new GitLabProjectBranchesService(timeUtility);
    }

    @Test
    public void shouldReturnProjectFromGitlabApi() throws Exception {
        // when
        GitlabProject gitlabProject = branchesService.findGitlabProjectForRepositoryUrl(
                gitlabApi, "git@git.example.com:groupOne/A.git");

        // then
        assertThat(gitlabProject, is(gitlabProjectA));
    }

    @Test
    public void shouldReturnBranchNamesFromGitlabApi() throws Exception {
        // when
        List<String> actualBranchNames = branchesService.getBranches(gitlabApi, "git@git.example.com:groupOne/B.git");

        // then
        assertThat(actualBranchNames, is(branchNamesProjectB));
    }

    @Test
    public void shouldNotCallGitlabApiGetProjectsWhenElementIsCached() throws Exception {
        // when
        branchesService.findGitlabProjectForRepositoryUrl(gitlabApi, "git@git.example.com:groupOne/A.git");
        verify(gitlabApi, times(1)).getProjects();
        branchesService.findGitlabProjectForRepositoryUrl(gitlabApi, "git@git.example.com:groupOne/B.git");

        // then
        verify(gitlabApi, times(1)).getProjects();
    }

    @Test
    public void shouldCallGitlabApiGetProjectsWhenElementIsNotCached() throws Exception {
        // when
        branchesService.findGitlabProjectForRepositoryUrl(gitlabApi, "git@git.example.com:groupOne/A.git");
        verify(gitlabApi, times(1)).getProjects();
        branchesService.findGitlabProjectForRepositoryUrl(gitlabApi, "git@git.example.com:groupOne/DoesNotExist.git");

        // then
        verify(gitlabApi, times(2)).getProjects();
    }

    @Test
    public void shoulNotCallGitlabApiGetBranchesWhenElementIsCached() throws Exception {
        // when
        branchesService.getBranches(gitlabApi, "git@git.example.com:groupOne/B.git");
        verify(gitlabApi, times(1)).getBranches(gitlabProjectB);
        branchesService.getBranches(gitlabApi, "git@git.example.com:groupOne/B.git");

        // then
        verify(gitlabApi, times(1)).getProjects();
    }

    @Test
    public void shoulNotMakeUnnecessaryCallsToGitlabApiGetBranches() throws Exception {
        // when
        branchesService.getBranches(gitlabApi, "git@git.example.com:groupOne/A.git");

        // then
        verify(gitlabApi, times(1)).getBranches(gitlabProjectA);
        verify(gitlabApi, times(0)).getBranches(gitlabProjectB);
    }

    @Test
    public void shouldExpireBranchCacheAtSetTime() throws Exception {
        // first call should retrieve branches from gitlabApi
        branchesService.getBranches(gitlabApi, "git@git.example.com:groupOne/A.git");
        verify(gitlabApi, times(1)).getBranches(gitlabProjectA);

        long timeAfterCacheExpiry = GitLabProjectBranchesService.BRANCH_CACHE_TIME_IN_MILLISECONDS + 2;
        when(timeUtility.getCurrentTimeInMillis()).thenReturn(timeAfterCacheExpiry);
        branchesService.getBranches(gitlabApi, "git@git.example.com:groupOne/A.git");

        // then
        verify(gitlabApi, times(2)).getBranches(gitlabProjectA);
    }

    @Test
    public void shouldExpireProjectCacheAtSetTime() throws Exception {
        // first call should retrieve projects from gitlabApi
        branchesService.findGitlabProjectForRepositoryUrl(gitlabApi, "git@git.example.com:groupOne/A.git");
        verify(gitlabApi, times(1)).getProjects();

        long timeAfterCacheExpiry = GitLabProjectBranchesService.PROJECT_MAP_CACHE_TIME_IN_MILLISECONDS + 2;
        when(timeUtility.getCurrentTimeInMillis()).thenReturn(timeAfterCacheExpiry);
        branchesService.findGitlabProjectForRepositoryUrl(gitlabApi, "git@git.example.com:groupOne/A.git");

        // then
        verify(gitlabApi, times(2)).getProjects();
    }

    /**
     * mocks calls to gitlabApi and GitlabAPI.getProjects and GitlabAPI.getBranches(gitlabProject)
     *
     * projectList has to have the size as the branchNamesList list.
     *
     * Each branchNamesList entry is a list of strings that is used to create a list of GitlabBranch elements; that list
     * is then returned for each gitlabProject.
     *
     * @param projectList
     *            returned for GitlabAPI.getProjects
     * @param branchNamesList
     *            an array of lists of branch names used to mock getBranches
     * @throws IOException
     */
    private void mockGitlab(List<GitlabProject> projectList, List<List<String>> branchNamesList) throws IOException {
        // mock the actual API
        gitlabApi = mock(GitlabAPI.class);

        when(gitlabApi.getProjects()).thenReturn(projectList);

        List<GitlabBranch> branchList;
        for (int i = 0; i < branchNamesList.size(); i++) {
            branchList = createGitlabBranches(projectList.get(i), branchNamesList.get(1));
            when(gitlabApi.getBranches(projectList.get(i))).thenReturn(branchList);
        }
    }

    private List<GitlabBranch> createGitlabBranches(GitlabProject gitlabProject, List<String> branchNames) {
        List<GitlabBranch> branches = new ArrayList<GitlabBranch>();
        GitlabBranch branch;
        for (String branchName : branchNames) {
            branch = new GitlabBranch();
            branch.setName(branchName);
            branches.add(branch);
        }
        return branches;
    }

    private GitlabProject setupGitlabProject(String namespace, String name) {
        GitlabProject project = new GitlabProject();
        project.setPathWithNamespace(namespace + "/" + name);
        project.setHttpUrl("http://git.example.com/" + project.getPathWithNamespace() + ".git");
        project.setSshUrl("git@git.example.com:" + project.getPathWithNamespace() + ".git");
        project.setName(name);
        GitlabNamespace gitNameSpace = new GitlabNamespace();
        gitNameSpace.setName(namespace);
        gitNameSpace.setPath(namespace);
        project.setNamespace(gitNameSpace);
        return project;
    }

}
