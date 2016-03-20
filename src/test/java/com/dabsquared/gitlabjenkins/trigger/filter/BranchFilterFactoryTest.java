package com.dabsquared.gitlabjenkins.trigger.filter;

import org.junit.Test;

import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * @author Robin Müller
 */
public class BranchFilterFactoryTest {

    @Test
    public void getAllBranchesFilter() {
        BranchFilter branchFilter = BranchFilterFactory.newBranchFilter(branchFilterConfig()
                .withIncludeBranchesSpec("master")
                .withExcludeBranchesSpec("develop")
                .withTargetBranchRegex(".*")
                .build(BranchFilterType.All));

        assertThat(branchFilter, instanceOf(AllBranchesFilter.class));
    }

    @Test
    public void getNameBasedFilterFilter() {
        BranchFilter branchFilter = BranchFilterFactory.newBranchFilter(branchFilterConfig()
                .withIncludeBranchesSpec("master")
                .withExcludeBranchesSpec("develop")
                .withTargetBranchRegex(".*")
                .build(BranchFilterType.NameBasedFilter));

        assertThat(branchFilter, instanceOf(NameBasedFilter.class));
    }

    @Test
    public void getRegexBasedFilterFilter() {
        BranchFilter branchFilter = BranchFilterFactory.newBranchFilter(branchFilterConfig()
                .withIncludeBranchesSpec("master")
                .withExcludeBranchesSpec("develop")
                .withTargetBranchRegex(".*")
                .build(BranchFilterType.RegexBasedFilter));

        assertThat(branchFilter, instanceOf(RegexBasedFilter.class));
    }
}
