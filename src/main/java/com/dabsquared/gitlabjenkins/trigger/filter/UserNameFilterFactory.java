package com.dabsquared.gitlabjenkins.trigger.filter;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Robin Müller
 */
public final class UserNameFilterFactory {

    private UserNameFilterFactory() { }

    private static final Logger LOGGER = Logger.getLogger(UserNameFilterFactory.class.getName());

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
