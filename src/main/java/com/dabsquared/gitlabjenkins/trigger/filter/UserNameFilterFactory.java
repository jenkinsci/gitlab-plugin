package com.dabsquared.gitlabjenkins.trigger.filter;

/**
 * @author jean flores
 */
public final class UserNameFilterFactory {

    private UserNameFilterFactory() { }

    public static UserNameFilter newUserNameFilter(UserNameFilterConfig config) {

		if(config == null)
			return new AllUserNamesFilter();
		
        switch (config.getType()) {
            case NameBasedFilter:
                return new UserNameBasedFilter(config.getExcludeUserNamesSpec());
            default:
                return new AllUserNamesFilter();
        }
    }
}
