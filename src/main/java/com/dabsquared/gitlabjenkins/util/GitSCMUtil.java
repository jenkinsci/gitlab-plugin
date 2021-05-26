package com.dabsquared.gitlabjenkins.util;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jgit.transport.URIish;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.WebHook;

import hudson.model.Item;
import hudson.plugins.git.GitSCM;
import jenkins.triggers.SCMTriggerItem;

public final class GitSCMUtil {

    private static final Logger LOGGER = Logger.getLogger (GitSCMUtil.class.getName ());

    private GitSCMUtil () {
    }

    public static boolean isConfiguredGitRepository (Item item,
            WebHook webhook) {
        Set<String> webhookUrlStrings = new HashSet<>();
        Set<URIish> webhookUrls = new HashSet<>();
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

        return itemScmContainsAnyUrl(item, webhookUrls);
    }

  private static boolean itemScmContainsAnyUrl(Item item, Set<URIish> urls) {
    SCMTriggerItem scmItem = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(item);
    if (scmItem != null) {
      return scmItem.getSCMs().stream()
                              .filter(scm -> scm instanceof GitSCM)
                              .flatMap(scm -> ((GitSCM) scm).getRepositories()
                                                            .stream()
                                                            .flatMap(repo -> repo.getURIs().stream()))
                              .anyMatch(urls::contains);
    }
    return false;
  }
}
