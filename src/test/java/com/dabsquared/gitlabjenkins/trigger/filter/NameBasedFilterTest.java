package com.dabsquared.gitlabjenkins.trigger.filter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

/**
 * @author Robin Müller
 */
public class NameBasedFilterTest {

	@Test
	public void includeBranches() {
		NameBasedFilter nameBasedFilter = new NameBasedFilter("master, develop", "");

		assertThat(nameBasedFilter.isBranchAllowed(null, "master"), is(true));
		assertThat(nameBasedFilter.isBranchAllowed(null, "develop"), is(true));
		assertThat(nameBasedFilter.isBranchAllowed(null, "not-included-branch"), is(false));
	}

	@Test
	public void excludeBranches() {
		NameBasedFilter nameBasedFilter = new NameBasedFilter("", "master, develop");

		assertThat(nameBasedFilter.isBranchAllowed(null, "master"), is(false));
		assertThat(nameBasedFilter.isBranchAllowed(null, "develop"), is(false));
		assertThat(nameBasedFilter.isBranchAllowed(null, "not-excluded-branch"), is(true));
	}

	@Test
	public void includeAndExcludeBranches() {
		NameBasedFilter nameBasedFilter = new NameBasedFilter("master", "develop");

		assertThat(nameBasedFilter.isBranchAllowed(null, "master"), is(true));
		assertThat(nameBasedFilter.isBranchAllowed(null, "develop"), is(false));
		assertThat(nameBasedFilter.isBranchAllowed(null, "not-excluded-and-not-included-branch"), is(false));
	}

	@Test
	public void allowIncludeAndExcludeToBeNull() {
		NameBasedFilter nameBasedFilter = new NameBasedFilter(null, null);

		assertThat(nameBasedFilter.isBranchAllowed(null, "master"), is(true));
	}

	@Test
	public void allowIncludeToBeNull() {
		NameBasedFilter nameBasedFilter = new NameBasedFilter(null, "master, develop");

		assertThat(nameBasedFilter.isBranchAllowed(null, "master"), is(false));
		assertThat(nameBasedFilter.isBranchAllowed(null, "develop"), is(false));
		assertThat(nameBasedFilter.isBranchAllowed(null, "not-excluded-branch"), is(true));
	}

	@Test
	public void allowExcludeToBeNull() {
		NameBasedFilter nameBasedFilter = new NameBasedFilter("master, develop", null);

		assertThat(nameBasedFilter.isBranchAllowed(null, "master"), is(true));
		assertThat(nameBasedFilter.isBranchAllowed(null, "develop"), is(true));
		assertThat(nameBasedFilter.isBranchAllowed(null, "not-included-branch"), is(false));
	}
}
