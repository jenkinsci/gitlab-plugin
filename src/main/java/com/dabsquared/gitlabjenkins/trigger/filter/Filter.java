package com.dabsquared.gitlabjenkins.trigger.filter;

/**
 * @author Robin Müller
 * @author Roland Hauser
 */
public interface Filter {

    boolean accept(String token);
}
