package com.dabsquared.gitlabjenkins.trigger.filter;

import org.junit.Test;

import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static com.dabsquared.gitlabjenkins.trigger.filter.UserNameFilterConfig.UserNameFilterConfigBuilder.userNameFilterConfig;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

public class UserNameFilterFactoryTest {

    @Test
    public void getAllUserNamesFilter() {
        UserNameFilter userNameFilter = UserNameFilterFactory.newUserNameFilter(userNameFilterConfig()
            .withExcludeUserNamesSpec("John Doe")
            .build(UserNameFilterType.All)
        );
        assertThat(userNameFilter, instanceOf(AllBranchesFilter.class));
    }
    @Test
    public void getUserNameBasedFilterFilter() {
        UserNameFilter userNameFilter = UserNameFilterFactory.newUserNameFilter(userNameFilterConfig()
            .withExcludeUserNamesSpec("John Doe")
            .build(UserNameFilterType.NameBasedFilter));

        assertThat(userNameFilter, instanceOf(NameBasedFilter.class));
    }
}
