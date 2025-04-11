package com.dabsquared.gitlabjenkins.trigger.filter;

import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Robin Müller
 */
class BranchFilterFactoryTest {

    @Test
    void getAllBranchesFilter() {
        BranchFilter branchFilter = BranchFilterFactory.newBranchFilter(branchFilterConfig()
                .withIncludeBranchesSpec("master")
                .withExcludeBranchesSpec("develop")
                .withTargetBranchRegex(".*")
                .build(BranchFilterType.All));

        assertThat(branchFilter, instanceOf(AllBranchesFilter.class));
    }

    @Test
    void getNameBasedFilterFilter() {
        BranchFilter branchFilter = BranchFilterFactory.newBranchFilter(branchFilterConfig()
                .withIncludeBranchesSpec("master")
                .withExcludeBranchesSpec("develop")
                .withTargetBranchRegex(".*")
                .build(BranchFilterType.NameBasedFilter));

        assertThat(branchFilter, instanceOf(NameBasedFilter.class));
    }

    @Test
    void getRegexBasedFilterFilter() {
        BranchFilter branchFilter = BranchFilterFactory.newBranchFilter(branchFilterConfig()
                .withIncludeBranchesSpec("master")
                .withExcludeBranchesSpec("develop")
                .withTargetBranchRegex(".*")
                .build(BranchFilterType.RegexBasedFilter));

        assertThat(branchFilter, instanceOf(RegexBasedFilter.class));
    }
}
