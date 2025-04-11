package com.dabsquared.gitlabjenkins.trigger.filter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * @author Robin Müller
 */
class MergeRequestLabelFilterImplTest {

    @Test
    void includeLabels() {
        MergeRequestLabelFilterImpl mergeRequestLabelFilter = new MergeRequestLabelFilterImpl("include, include2", "");

        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton("include")), is(true));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton("include2")), is(true));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton("other-label")), is(false));
    }

    @Test
    void excludeLabels() {
        MergeRequestLabelFilterImpl mergeRequestLabelFilter = new MergeRequestLabelFilterImpl("", "exclude, exclude2");

        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton("exclude")), is(false));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton("exclude2")), is(false));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton("other-label")), is(true));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.emptySet()), is(true));
    }

    @Test
    void includeAndExcludeLabels() {
        MergeRequestLabelFilterImpl mergeRequestLabelFilter =
                new MergeRequestLabelFilterImpl("include, include2", "exclude, exclude2");

        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton("include")), is(true));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton("include2")), is(true));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton("exclude")), is(false));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton("exclude2")), is(false));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton("other-label")), is(false));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Arrays.asList("include", "exclude")), is(false));
    }
}
