package com.dabsquared.gitlabjenkins.trigger.filter;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
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
        assertThat(nameBasedFilter.isBranchAllowed("not-included-branch"), is(false));
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
    public void allowIncludeAndExcludeToBeNull() {
      NameBasedFilter nameBasedFilter = new NameBasedFilter(null, null);

      assertThat(nameBasedFilter.isBranchAllowed("master"), is(true));
    }

    @Test
    public void allowIncludeToBeNull() {
      NameBasedFilter nameBasedFilter = new NameBasedFilter(null, "master, develop");

      assertThat(nameBasedFilter.isBranchAllowed("master"), is(false));
      assertThat(nameBasedFilter.isBranchAllowed("develop"), is(false));
      assertThat(nameBasedFilter.isBranchAllowed("not-excluded-branch"), is(true));
    }

    @Test
    public void allowExcludeToBeNull() {
      NameBasedFilter nameBasedFilter = new NameBasedFilter("master, develop", null);

      assertThat(nameBasedFilter.isBranchAllowed("master"), is(true));
      assertThat(nameBasedFilter.isBranchAllowed("develop"), is(true));
      assertThat(nameBasedFilter.isBranchAllowed("not-included-branch"), is(false));
    }
}
