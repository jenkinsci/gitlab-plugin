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

        assertThat(nameBasedFilter.accept("master"), is(true));
        assertThat(nameBasedFilter.accept("develop"), is(true));
    }

    @Test
    public void excludeBranches() {
        NameBasedFilter nameBasedFilter = new NameBasedFilter("", "master, develop");

        assertThat(nameBasedFilter.accept("master"), is(false));
        assertThat(nameBasedFilter.accept("develop"), is(false));
        assertThat(nameBasedFilter.accept("not-excluded-branch"), is(true));
    }

    @Test
    public void includeAndExcludeBranches() {
        NameBasedFilter nameBasedFilter = new NameBasedFilter("master", "develop");

        assertThat(nameBasedFilter.accept("master"), is(true));
        assertThat(nameBasedFilter.accept("develop"), is(false));
        assertThat(nameBasedFilter.accept("not-excluded-and-not-included-branch"), is(false));
    }
}
