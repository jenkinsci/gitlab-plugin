package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import com.dabsquared.gitlabjenkins.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.traits.IgnoreOnPushNotificationTrait;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.trait.SCMTrait;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.URIish;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.util.JsonUtil.toPrettyPrint;
import static com.dabsquared.gitlabjenkins.util.LoggerUtil.toArray;

/**
 * @author Robin Müller
 */
public class PushBuildAction extends BuildWebHookAction {

    private final static Logger LOGGER = Logger.getLogger(PushBuildAction.class.getName());
    private final Item project;
    private PushHook pushHook;
    private final String secretToken;

    public PushBuildAction(Item project, String json, String secretToken) {
        LOGGER.log(Level.FINE, "Push: {0}", toPrettyPrint(json));
        this.project = project;
        this.pushHook = JsonUtil.read(json, PushHook.class);
        this.secretToken = secretToken;
    }

    /**
     * Alternative Constructor which takes in an already deserialized Json Tree.
     * @param project Jenkins Project Item
     * @param json Payload Json Tree
     * @param secretToken Secret Token
     */
    public PushBuildAction(Item project, JsonNode json, String secretToken) {
        LOGGER.log(Level.FINE, "Push: {0}", toPrettyPrint(json));
        this.project = project;
        this.pushHook = JsonUtil.read(json, PushHook.class);
        this.secretToken = secretToken;
    }

    void processForCompatibility() {
        // Fill in project if it's not defined.
        if (this.pushHook.getProject() == null && this.pushHook.getRepository() != null) {
            try {
                String path = new URL(this.pushHook.getRepository().getGitHttpUrl()).getPath();
                if (StringUtils.isNotBlank(path)) {
                    Project project = new Project();
                    project.setNamespace(path.replaceFirst("/", "").substring(0, path.lastIndexOf("/")));
                    this.pushHook.setProject(project);
                } else {
                    LOGGER.log(Level.WARNING, "Could not find suitable namespace.");
                }
            } catch (MalformedURLException ignored) {
                LOGGER.log(Level.WARNING, "Invalid repository url found while building namespace.");
            }
        }
    }

    public void execute() {
        if (pushHook.getRepository() != null && pushHook.getRepository().getUrl() == null) {
            LOGGER.log(Level.WARNING, "No repository url found.");
            return;
        }

        if (project instanceof Job<?, ?>) {
            ACL.impersonate(ACL.SYSTEM, new TriggerNotifier(project, secretToken, Jenkins.getAuthentication()) {
                @Override
                protected void performOnPost(GitLabPushTrigger trigger) {
                    trigger.onPost(pushHook);
                }
            });
            throw HttpResponses.ok();
        }
        if (project instanceof SCMSourceOwner) {
            ACL.impersonate(ACL.SYSTEM, new SCMSourceOwnerNotifier());
            throw HttpResponses.ok();
        }
        throw HttpResponses.errorWithoutStack(409, "Push Hook is not supported for this project");
    }

    private class SCMSourceOwnerNotifier implements Runnable {
        public void run() {
            for (SCMSource scmSource : ((SCMSourceOwner) project).getSCMSources()) {
                if (scmSource instanceof GitSCMSource) {
                    GitSCMSource gitSCMSource = (GitSCMSource) scmSource;
                    try {
                        if (new URIish(gitSCMSource.getRemote()).equals(new URIish(gitSCMSource.getRemote()))) {
                            if (SCMTrait.find(gitSCMSource.getTraits(), IgnoreOnPushNotificationTrait.class) != null) {
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

}
