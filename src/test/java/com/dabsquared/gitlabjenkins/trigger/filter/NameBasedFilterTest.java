package com.dabsquared.gitlabjenkins.trigger.filter;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Robin Müller
 */
public class NameBasedFilterTest {

    @Test
    public void includeBranches() {
        NameBasedFilter nameBasedFilter = new NameBasedFilter("master, develop", "");

        assertThat(nameBasedFilter.isBranchAllowed("master"), is(true));
        assertThat(nameBasedFilter.isBranchAllowed("develop"), is(true));
    }

    @Test
    public void excludeBranches() {
        NameBasedFilter nameBasedFilter = new NameBasedFilter("", "master, develop");

        assertThat(nameBasedFilter.isBranchAllowed("master"), is(false));
        assertThat(nameBasedFilter.isBranchAllowed("develop"), is(false));
        assertThat(nameBasedFilter.isBranchAllowed("not-excluded-branch"), is(true));
    }

    @Test
    public void includeAndExcludeBranches() {
        NameBasedFilter nameBasedFilter = new NameBasedFilter("master", "develop");

        assertThat(nameBasedFilter.isBranchAllowed("master"), is(true));
        assertThat(nameBasedFilter.isBranchAllowed("develop"), is(false));
        assertThat(nameBasedFilter.isBranchAllowed("not-excluded-and-not-included-branch"), is(false));
    }

    @Test
    public void getConfig() {
        String includedBranches = "master, develop";
        String excludedBranches = "hotfix/test";

        BranchFilterConfig config = new NameBasedFilter(includedBranches, excludedBranches).getConfig();

        assertThat(config.getType(), is(BranchFilterType.NameBasedFilter));
        assertThat(config.getIncludeBranchesSpec(), is(includedBranches));
        assertThat(config.getExcludeBranchesSpec(), is(excludedBranches));
        assertThat(config.getTargetBranchRegex(), nullValue());
    }
}
