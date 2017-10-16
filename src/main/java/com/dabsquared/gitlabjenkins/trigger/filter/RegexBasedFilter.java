package com.dabsquared.gitlabjenkins.trigger.filter;

import com.dabsquared.gitlabjenkins.util.LoggerUtil;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author Robin Müller
 * @author Roland Hauser
 */
class RegexBasedFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(RegexBasedFilter.class.getName());

    private final Pattern regex;

    public RegexBasedFilter(String regex) {
        this.regex = isEmpty(regex) ? null : compile(regex);
    }

    @Override
    public boolean accept(String token) {
        if (isEmpty(token)) {
            LOGGER.finer("Token is empty, accept = true");
            return true;
        } else if (regex == null) {
            LOGGER.finer("Pattern is empty, accept = true");
            return true;
        } else if (regex.matcher(token).matches()) {
            LOGGER.log(Level.FINER, "{0} matches {1}, accept = true", LoggerUtil.toArray(token, regex));
            return true;
        }
        LOGGER.log(Level.FINER, "{0} does not match {1}, accept = false", LoggerUtil.toArray(token, regex));
        return false;
    }
}
