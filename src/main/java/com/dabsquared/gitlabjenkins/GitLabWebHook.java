package com.dabsquared.gitlabjenkins;

import hudson.ExtensionPoint;
import hudson.model.Hudson;
import hudson.model.RootAction;
import hudson.model.UnprotectedRootAction;

import java.util.logging.Logger;

/**
 *
 * @author Daniel Brooks
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


    private static final Logger LOGGER = Logger.getLogger(GitLabWebHook.class.getName());

    public static GitLabWebHook get() {
        return Hudson.getInstance().getExtensionList(RootAction.class).get(GitLabWebHook.class);
    }

    /**
     * Other plugins may be interested in listening for these updates.
     *
     * @since 1.8
     */
    public static abstract class Listener implements ExtensionPoint {

        /**
         * Called when there is a change notification on a specific repository.
         *
         * @param pusherName        the pusher name.
         * @param changedRepository the changed repository.
         * @since 1.8
         */
        public abstract void onPushRepositoryChanged(String pusherName, String changedRepository);
    }


}
