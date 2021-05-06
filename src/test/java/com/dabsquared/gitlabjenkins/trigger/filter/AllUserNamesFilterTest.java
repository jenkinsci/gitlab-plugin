package com.dabsquared.gitlabjenkins.trigger.filter;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author jean.flores
 */

public class AllUserNamesFilterTest{

    @Test
    public void isRandomBranchNameAllowed() {
        String randomUserName = RandomStringUtils.random(10, true, false);
        assertThat(new AllUserNamesFilter().isUserNameAllowed(randomUserName), is(true));
    }
}
