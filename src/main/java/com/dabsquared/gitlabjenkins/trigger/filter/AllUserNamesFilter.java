package com.dabsquared.gitlabjenkins.trigger.filter;

/**
 * @author Robin Müller
 */
class AllUserNamesFilter implements UserNameFilter {
    @Override
    public boolean isUserNameAllowed(String userName) {
        return true;
    }
}
