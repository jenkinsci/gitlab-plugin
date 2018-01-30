package com.dabsquared.gitlabjenkins.trigger.filter;

import com.dabsquared.gitlabjenkins.gitlab.api.model.Label;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Robin Müller
 */
public class MergeRequestLabelFilterImplTest {
    Label include = new Label();
    Label include2 = new Label();
    Label other = new Label();
    Label exclude = new Label();
    Label exclude2 = new Label();
    Label other2 = new Label();


    @Before
    public void setUp() {
        include.setTitle("include");
        include2.setTitle("include2");
        other.setTitle("other");
        exclude.setTitle("exclude");
        exclude2.setTitle("exclude2");
        other2.setTitle("other-label");
    }

    @Test
    public void includeLabels() {
        MergeRequestLabelFilterImpl mergeRequestLabelFilter = new MergeRequestLabelFilterImpl("include, include2", "");

        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton(include)), is(true));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton(include2)), is(true));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton(other)), is(false));
    }

    @Test
    public void excludeLabels() {
        MergeRequestLabelFilterImpl mergeRequestLabelFilter = new MergeRequestLabelFilterImpl("", "exclude, exclude2");

        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton(exclude)), is(false));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton(exclude2)), is(false));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton(other2)), is(true));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.<Label>emptySet()), is(true));
    }

    @Test
    public void includeAndExcludeLabels() {
        MergeRequestLabelFilterImpl mergeRequestLabelFilter = new MergeRequestLabelFilterImpl("include, include2", "exclude, exclude2");

        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton(include)), is(true));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton(include2)), is(true));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton(exclude)), is(false));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton(exclude)), is(false));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Collections.singleton(other2)), is(false));
        assertThat(mergeRequestLabelFilter.isMergeRequestAllowed(Arrays.asList(include, exclude)), is(false));
    }
}
