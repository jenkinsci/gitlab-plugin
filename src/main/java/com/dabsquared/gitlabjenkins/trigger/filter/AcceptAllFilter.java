package com.dabsquared.gitlabjenkins.trigger.filter;

import java.util.logging.Logger;

/**
 * @author Robin Müller
 * @author Roland Hauser
 */
class AcceptAllFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(AcceptAllFilter.class.getName());

    @Override
    public boolean accept(String unused) {
        LOGGER.finer("{0}, accept = true");
        return true;
    }
}
