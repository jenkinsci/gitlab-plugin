package com.dabsquared.gitlabjenkins.util;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import com.dabsquared.gitlabjenkins.Messages;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.WebHook;

import hudson.model.Item;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import jenkins.triggers.SCMTriggerItem;

public final class GitSCMUtil {

    private static final Logger LOGGER = Logger.getLogger (GitSCMUtil.class.getName ());

    private GitSCMUtil () {
    }

    public static boolean isConfiguredGitRepository (Item item,
            WebHook webhook) {
        boolean isConfigured = false;
        List<String> webhookUrlStrings = new ArrayList<>();
        List<URIish> webhookUrls = new ArrayList<>();
        if (webhook.getProject () != null) {
            webhookUrlStrings.add (webhook.getProject ().getGitHttpUrl ());
            webhookUrlStrings.add (webhook.getProject ().getGitSshUrl ());
            webhookUrlStrings.add (webhook.getProject ().getUrl ());
        }
        if (webhook instanceof MergeRequestHook) {
            webhookUrlStrings.add (((MergeRequestHook) webhook).getObjectAttributes ().getTarget ().getGitHttpUrl ());
            webhookUrlStrings.add (((MergeRequestHook) webhook).getObjectAttributes ().getTarget ().getGitSshUrl ());
            webhookUrlStrings.add (((MergeRequestHook) webhook).getObjectAttributes ().getTarget ().getUrl ());
        }

        for (String s : webhookUrlStrings) {
            if (s == null || s.isEmpty ()){
                continue;
            }

            try {
                webhookUrls.add (new URIish(s));
            } catch (URISyntaxException e) {
                LOGGER.log (Level.INFO, "Malformed URI in webhook: " + e.getMessage ());
            }
        }

        try {
            for (URIish uri: getConfiguredGitURIs (item)) {
                for (URIish webhookUrl : webhookUrls) {
                    if (uri.equals(webhookUrl)) {
                        isConfigured = true;
                    }
                }
            }
        } catch (IllegalStateException e) {
            return false;
        }

        return isConfigured;
    }

    public static List<URIish> getConfiguredGitURIs (Item item) {
        SCMTriggerItem scmItem = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem (item);
        GitSCM gitSCM = getGitSCM (scmItem);
        if (gitSCM == null) {
            LOGGER.log (Level.FINE, "Could not find GitSCM for project. Project = {0}", item.getName ());
            throw new IllegalStateException ("This project does not use git:" + item.getName ());
        }

        List<RemoteConfig> repositories = gitSCM.getRepositories ();
        if (!repositories.isEmpty ()) {
            List<URIish> uris = new ArrayList<> ();
            for (RemoteConfig repo: repositories) {
                uris.addAll (repo.getURIs ());
            }
            if (!uris.isEmpty ()) {
                return uris;
            }
        }
        throw new IllegalStateException (Messages.GitLabPushTrigger_NoSourceRepository ());
    }

    public static GitSCM getGitSCM (SCMTriggerItem item) {
        if (item != null) {
            for (SCM scm: item.getSCMs ()) {
                if (scm instanceof GitSCM) {
                    return (GitSCM) scm;
                }
            }
        }
        return null;
    }
}
