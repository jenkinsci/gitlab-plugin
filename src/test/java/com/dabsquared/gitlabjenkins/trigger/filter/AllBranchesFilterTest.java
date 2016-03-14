package com.dabsquared.gitlabjenkins.trigger.filter;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Robin Müller
 */
public class AllBranchesFilterTest {

    @Test
    public void isRandomBranchNameAllowed() {
        String randomBranchName = RandomStringUtils.random(10, true, false);

        assertThat(new AllBranchesFilter().isBranchAllowed(randomBranchName), is(true));
    }

    @Test
    public void getConfig() {
        BranchFilterConfig config = new AllBranchesFilter().getConfig();

        assertThat(config.getType(), is(BranchFilterType.All));
        assertThat(config.getExcludeBranchesSpec(), nullValue());
        assertThat(config.getIncludeBranchesSpec(), nullValue());
        assertThat(config.getTargetBranchRegex(), nullValue());
    }
}
