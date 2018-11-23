package com.dabsquared.gitlabjenkins.webhook.build;

import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.transport.URIish;

import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import hudson.model.Item;

import static com.dabsquared.gitlabjenkins.util.LoggerUtil.toArray;

public class SCMSourceOwnerNotifier implements Runnable{

    private final static Logger LOGGER = Logger.getLogger(PushBuildAction.class.getName());
    private final Item project;

    public SCMSourceOwnerNotifier(Item project) {
        this.project = project;
    }

    public void run() {
        for (SCMSource scmSource : ((SCMSourceOwner) project).getSCMSources()) {
            if (scmSource instanceof GitSCMSource) {
                GitSCMSource gitSCMSource = (GitSCMSource) scmSource;
                try {
                    if (new URIish(gitSCMSource.getRemote()).equals(new URIish(gitSCMSource.getRemote()))) {
                        if (!gitSCMSource.isIgnoreOnPushNotifications()) {
                            LOGGER.log(Level.FINE, "Notify scmSourceOwner {0} about changes for {1}",
                                toArray(project.getName(), gitSCMSource.getRemote()));
                            ((SCMSourceOwner) project).onSCMSourceUpdated(scmSource);
                        } else {
                            LOGGER.log(Level.FINE, "Ignore on push notification for scmSourceOwner {0} about changes for {1}",
                                toArray(project.getName(), gitSCMSource.getRemote()));
                        }
                    }
                } catch (URISyntaxException e) {
                    // nothing to do
                }
            }
        }
    }
}
