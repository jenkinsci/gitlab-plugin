package com.dabsquared.gitlabjenkins.trigger.filter;

import org.apache.commons.lang.StringUtils;

/**
 * @author Robin Müller
 */
class RegexBasedFilter implements Filter {

    private final String regex;

    public RegexBasedFilter(String regex) {
        this.regex = regex;
    }

    @Override
    public boolean accept(String token) {
        return StringUtils.isEmpty(token) || StringUtils.isEmpty(regex) || token.matches(regex);
    }
}
