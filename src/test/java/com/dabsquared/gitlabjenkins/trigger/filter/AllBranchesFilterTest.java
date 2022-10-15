package com.dabsquared.gitlabjenkins.trigger.filter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

/**
 * @author Robin Müller
 */
public class AllBranchesFilterTest {

    @Test
    public void isRandomBranchNameAllowed() {
        String randomBranchName = RandomStringUtils.random(10, true, false);

        assertThat(new AllBranchesFilter().isBranchAllowed(null, randomBranchName), is(true));
    }
}
