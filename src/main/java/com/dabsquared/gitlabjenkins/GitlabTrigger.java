package com.dabsquared.gitlabjenkins;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractProject;

import java.util.Collection;
import java.util.Set;

/**
 * Optional interface that can be implemented by {@link hudson.triggers.Trigger} that watches out for a change in GitHub
 * and triggers a build.
 *
 * @author Daniel Brooks
 */
public interface GitLabTrigger {

    @Deprecated
    public void onPost();

    // TODO: document me
    public void onPost(String triggeredByUser);

}