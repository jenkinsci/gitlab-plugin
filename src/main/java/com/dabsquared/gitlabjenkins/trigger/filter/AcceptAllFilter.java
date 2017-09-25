package com.dabsquared.gitlabjenkins.trigger.filter;

/**
 * @author Robin Müller
 * @author Roland Hauser
 */
class AcceptAllFilter implements Filter {
    @Override
    public boolean accept(String unused) {
        return true;
    }
}
