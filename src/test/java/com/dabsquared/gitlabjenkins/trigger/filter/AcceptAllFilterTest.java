package com.dabsquared.gitlabjenkins.trigger.filter;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Robin Müller
 */
public class AcceptAllFilterTest {

    @Test
    public void isRandomBranchNameAllowed() {
        String randomBranchName = RandomStringUtils.random(10, true, false);

        assertThat(new AcceptAllFilter().accept(randomBranchName), is(true));
    }
}
