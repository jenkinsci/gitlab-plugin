package com.dabsquared.gitlabjenkins.trigger.filter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Robin Müller
 */
class NameBasedFilterTest {

    @Test
    void includeBranches() {
        NameBasedFilter nameBasedFilter = new NameBasedFilter("master, develop", "");

        assertThat(nameBasedFilter.isBranchAllowed(null, "master"), is(true));
        assertThat(nameBasedFilter.isBranchAllowed(null, "develop"), is(true));
        assertThat(nameBasedFilter.isBranchAllowed(null, "not-included-branch"), is(false));
    }

    @Test
    void excludeBranches() {
        NameBasedFilter nameBasedFilter = new NameBasedFilter("", "master, develop");

        assertThat(nameBasedFilter.isBranchAllowed(null, "master"), is(false));
        assertThat(nameBasedFilter.isBranchAllowed(null, "develop"), is(false));
        assertThat(nameBasedFilter.isBranchAllowed(null, "not-excluded-branch"), is(true));
    }

    @Test
    void includeAndExcludeBranches() {
        NameBasedFilter nameBasedFilter = new NameBasedFilter("master", "develop");

        assertThat(nameBasedFilter.isBranchAllowed(null, "master"), is(true));
        assertThat(nameBasedFilter.isBranchAllowed(null, "develop"), is(false));
        assertThat(nameBasedFilter.isBranchAllowed(null, "not-excluded-and-not-included-branch"), is(false));
    }

    @Test
    void allowIncludeAndExcludeToBeNull() {
        NameBasedFilter nameBasedFilter = new NameBasedFilter(null, null);

        assertThat(nameBasedFilter.isBranchAllowed(null, "master"), is(true));
    }

    @Test
    void allowIncludeToBeNull() {
        NameBasedFilter nameBasedFilter = new NameBasedFilter(null, "master, develop");

        assertThat(nameBasedFilter.isBranchAllowed(null, "master"), is(false));
        assertThat(nameBasedFilter.isBranchAllowed(null, "develop"), is(false));
        assertThat(nameBasedFilter.isBranchAllowed(null, "not-excluded-branch"), is(true));
    }

    @Test
    void allowExcludeToBeNull() {
        NameBasedFilter nameBasedFilter = new NameBasedFilter("master, develop", null);

        assertThat(nameBasedFilter.isBranchAllowed(null, "master"), is(true));
        assertThat(nameBasedFilter.isBranchAllowed(null, "develop"), is(true));
        assertThat(nameBasedFilter.isBranchAllowed(null, "not-included-branch"), is(false));
    }
}
