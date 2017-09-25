package com.dabsquared.gitlabjenkins.trigger.filter;

import org.junit.Test;

import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * @author Robin Müller
 * @author Roland Hauser
 */
public class FilterFactoryTest {

    @Test
    public void getAllBranchesFilter() {
        Filter branchFilter = FilterFactory.newBranchFilter(branchFilterConfig()
                .withIncludeBranchesSpec("master")
                .withExcludeBranchesSpec("develop")
                .withTargetBranchRegex(".*")
                .build(BranchFilterType.All));

        assertThat(branchFilter, instanceOf(AcceptAllFilter.class));
    }

    @Test
    public void getNameBasedFilterFilter() {
        Filter branchFilter = FilterFactory.newBranchFilter(branchFilterConfig()
                .withIncludeBranchesSpec("master")
                .withExcludeBranchesSpec("develop")
                .withTargetBranchRegex(".*")
                .build(BranchFilterType.NameBasedFilter));

        assertThat(branchFilter, instanceOf(NameBasedFilter.class));
    }

    @Test
    public void getRegexBasedFilterFilter() {
        Filter branchFilter = FilterFactory.newBranchFilter(branchFilterConfig()
                .withIncludeBranchesSpec("master")
                .withExcludeBranchesSpec("develop")
                .withTargetBranchRegex(".*")
                .build(BranchFilterType.RegexBasedFilter));

        assertThat(branchFilter, instanceOf(RegexBasedFilter.class));
    }

    @Test
    public void getAllFilesFilter() {
        Filter branchFilter = FilterFactory.newFilesFilter("");
        assertThat(branchFilter, instanceOf(AcceptAllFilter.class));

        branchFilter = FilterFactory.newFilesFilter(null);
        assertThat(branchFilter, instanceOf(AcceptAllFilter.class));
    }

    @Test
    public void getFilesFilter() {
        Filter branchFilter = FilterFactory.newFilesFilter(".*");

        assertThat(branchFilter, instanceOf(RegexBasedFilter.class));
    }
}
