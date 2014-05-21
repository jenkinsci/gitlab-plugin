package com.dabsquared.gitlabjenkins;

import hudson.model.UnprotectedRootAction;

/**
 * Created by Daniel on 5/20/14.
 */
public class GitLabWebHook implements UnprotectedRootAction {
    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return "/projects/";
    }
}
