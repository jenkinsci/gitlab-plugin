package com.dabsquared.gitlabjenkins.trigger.filter;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Robin Müller
 */
public class UserNameBasedFilterTest {

	@Test
	public void excludeBranches() {
		UserNameBasedFilter nameBasedFilter = new UserNameBasedFilter("John Doe, John Doe2");

		assertThat(nameBasedFilter.isUserNameAllowed("John Doe"), is(false));
		assertThat(nameBasedFilter.isUserNameAllowed("John Doe2"), is(false));
		assertThat(nameBasedFilter.isUserNameAllowed("not-excluded-user"), is(true));
	}

	@Test
	public void allowExcludeToBeNull() {
		UserNameBasedFilter nameBasedFilter = new UserNameBasedFilter(null);

		assertThat(nameBasedFilter.isUserNameAllowed("John Doe"), is(true));
	}
}
