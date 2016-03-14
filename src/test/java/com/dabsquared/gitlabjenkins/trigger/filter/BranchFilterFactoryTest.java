package com.dabsquared.gitlabjenkins.trigger.filter;

import org.junit.Test;

import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
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
        assertThat(branchFilter.getConfig().getIncludeBranchesSpec(), nullValue());
        assertThat(branchFilter.getConfig().getExcludeBranchesSpec(), nullValue());
        assertThat(branchFilter.getConfig().getTargetBranchRegex(), nullValue());
    }

    @Test
    public void getNameBasedFilterFilter() {
        String includeBranchesSpec = "master";
        String excludeBranchesSpec = "develop";
        BranchFilter branchFilter = BranchFilterFactory.newBranchFilter(branchFilterConfig()
                .withIncludeBranchesSpec(includeBranchesSpec)
                .withExcludeBranchesSpec(excludeBranchesSpec)
                .withTargetBranchRegex(".*")
                .build(BranchFilterType.NameBasedFilter));

        assertThat(branchFilter, instanceOf(NameBasedFilter.class));
        assertThat(branchFilter.getConfig().getIncludeBranchesSpec(), is(includeBranchesSpec));
        assertThat(branchFilter.getConfig().getExcludeBranchesSpec(), is(excludeBranchesSpec));
        assertThat(branchFilter.getConfig().getTargetBranchRegex(), nullValue());
    }

    @Test
    public void getRegexBasedFilterFilter() {
        String regex = ".*";
        BranchFilter branchFilter = BranchFilterFactory.newBranchFilter(branchFilterConfig()
                .withIncludeBranchesSpec("master")
                .withExcludeBranchesSpec("develop")
                .withTargetBranchRegex(regex)
                .build(BranchFilterType.RegexBasedFilter));

        assertThat(branchFilter, instanceOf(RegexBasedFilter.class));
        assertThat(branchFilter.getConfig().getIncludeBranchesSpec(), nullValue());
        assertThat(branchFilter.getConfig().getExcludeBranchesSpec(), nullValue());
        assertThat(branchFilter.getConfig().getTargetBranchRegex(), is(regex));
    }
}
