package com.dabsquared.gitlabjenkins.gitlab.api.model;

/**
 * @author Robin Müller
 */
public enum BuildState {
    pending,
    running,
    canceled,
    success,
    failed,
    skipped
}
