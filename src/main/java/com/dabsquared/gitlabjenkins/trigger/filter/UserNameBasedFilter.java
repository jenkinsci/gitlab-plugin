package com.dabsquared.gitlabjenkins.trigger.filter;

import com.google.common.base.Splitter;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * @author jean flores
 */
class UserNameBasedFilter implements UserNameFilter {
    private static final Logger LOGGER = Logger.getLogger(UserNameBasedFilter.class.getName());


    private final List<String> excludedUserNames;

    public UserNameBasedFilter(String excludedUserNames) {
        this.excludedUserNames = convert(excludedUserNames);
    }
    @Override
    public boolean isUserNameAllowed(String userName) {
        return hasNoUserNameSpecs() || isUserNameNotExcluded(userName);
    }

    private boolean hasNoUserNameSpecs() {
        return excludedUserNames.isEmpty();
    }

    private boolean isUserNameNotExcluded(String userName) {
        AntPathMatcher matcher = new AntPathMatcher();
        for (String excludePattern : excludedUserNames) {
            LOGGER.log(Level.INFO, "excludedUserNames");
            LOGGER.log(Level.INFO, excludePattern);
            LOGGER.log(Level.INFO, userName);
            if (matcher.match(excludePattern, userName)) {
                return false;
            }
        }
        return true;
    }

    private List<String> convert(String commaSeparatedString) {
        if (commaSeparatedString == null)
            return Collections.EMPTY_LIST;

        ArrayList<String> result = new ArrayList<>();
        for (String s : Splitter.on(',').omitEmptyStrings().trimResults().split(commaSeparatedString)) {
            result.add(s);
        }
        return result;
    }
}
