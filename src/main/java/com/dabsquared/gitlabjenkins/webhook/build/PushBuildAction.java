package com.dabsquared.gitlabjenkins.webhook.build;

import static com.dabsquared.gitlabjenkins.util.LoggerUtil.toArray;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.traits.IgnoreOnPushNotificationTrait;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.trait.SCMTrait;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.URIish;
import org.gitlab4j.api.systemhooks.PushSystemHookEvent;
import org.gitlab4j.api.systemhooks.TagPushSystemHookEvent;
import org.gitlab4j.api.webhook.EventProject;
import org.gitlab4j.api.webhook.PushEvent;
import org.gitlab4j.api.webhook.TagPushEvent;

/**
 * @author Robin Müller
 */
public class PushBuildAction extends BuildWebHookAction {

    private static final Logger LOGGER = Logger.getLogger(PushBuildAction.class.getName());
    private final Item project;
    private PushEvent pushEvent;
    private TagPushEvent tagPushEvent;
    private PushSystemHookEvent pushSystemHookEvent;
    private TagPushSystemHookEvent tagPushSystemHookEvent;
    private final String secretToken;

    public PushBuildAction(Item project, PushEvent pushEvent, String secretToken) {
        this.project = project;
        this.pushEvent = pushEvent;
        this.secretToken = secretToken;
    }

    public PushBuildAction(Item project, TagPushEvent tagPushEvent, String secretToken) {
        this.project = project;
        this.tagPushEvent = tagPushEvent;
        this.secretToken = secretToken;
    }

    public PushBuildAction(Item project, PushSystemHookEvent pushSystemHookEvent, String secretToken) {
        this.project = project;
        this.pushSystemHookEvent = pushSystemHookEvent;
        this.secretToken = secretToken;
    }

    public PushBuildAction(Item project, TagPushSystemHookEvent tagPushSystemHookEvent, String secretToken) {
        LOGGER.log(Level.FINE, tagPushSystemHookEvent.toString());
        this.project = project;
        this.tagPushSystemHookEvent = tagPushSystemHookEvent;
        this.secretToken = secretToken;
    }

    void processForCompatibility() {
        // Fill in project if it's not defined.
        if (this.pushEvent != null) {
            if (this.pushEvent.getProject() == null && this.pushEvent.getRepository() != null) {
                try {
                    String path = new URL(this.pushEvent.getRepository().getGit_http_url()).getPath();
                    if (StringUtils.isNotBlank(path)) {
                        EventProject project = new EventProject();
                        project.setNamespace(path.replaceFirst("/", "").substring(0, path.lastIndexOf("/")));
                        this.pushEvent.setProject(project);
                    } else {
                        LOGGER.log(Level.WARNING, "Could not find suitable namespace");
                    }
                } catch (MalformedURLException ignored) {
                    LOGGER.log(Level.WARNING, "Invalid repository url found while building namespace");
                }
            }
        }
        if (this.tagPushEvent != null) {
            if (this.tagPushEvent.getProject() == null && this.tagPushEvent.getRepository() != null) {
                try {
                    String path = new URL(this.tagPushEvent.getRepository().getGit_http_url()).getPath();
                    if (StringUtils.isNotBlank(path)) {
                        EventProject project = new EventProject();
                        project.setNamespace(path.replaceFirst("/", "").substring(0, path.lastIndexOf("/")));
                        this.tagPushEvent.setProject(project);
                    } else {
                        LOGGER.log(Level.WARNING, "Could not find suitable namespace");
                    }
                } catch (MalformedURLException ignored) {
                    LOGGER.log(Level.WARNING, "Invalid repository url found while building namespace");
                }
            }
        }
        if (this.pushSystemHookEvent != null) {
            if (this.pushSystemHookEvent.getProject() == null && this.pushSystemHookEvent.getRepository() != null) {
                try {
                    String path =
                            new URL(this.pushSystemHookEvent.getRepository().getGit_http_url()).getPath();
                    if (StringUtils.isNotBlank(path)) {
                        EventProject project = new EventProject();
                        project.setNamespace(path.replaceFirst("/", "").substring(0, path.lastIndexOf("/")));
                        this.pushSystemHookEvent.setProject(project);
                    } else {
                        LOGGER.log(Level.WARNING, "Could not find suitable namespace");
                    }
                } catch (MalformedURLException ignored) {
                    LOGGER.log(Level.WARNING, "Invalid repository url found while building namespace");
                }
            }
        }
        if (this.tagPushSystemHookEvent != null) {
            if (this.tagPushSystemHookEvent.getProject() == null
                    && this.tagPushSystemHookEvent.getRepository() != null) {
                try {
                    String path =
                            new URL(this.tagPushSystemHookEvent.getRepository().getGit_http_url()).getPath();
                    if (StringUtils.isNotBlank(path)) {
                        EventProject project = new EventProject();
                        project.setNamespace(path.replaceFirst("/", "").substring(0, path.lastIndexOf("/")));
                        this.tagPushSystemHookEvent.setProject(project);
                    } else {
                        LOGGER.log(Level.WARNING, "Could not find suitable namespace");
                    }
                } catch (MalformedURLException ignored) {
                    LOGGER.log(Level.WARNING, "Invalid repository url found while building namespace");
                }
            }
        }
    }

    public void execute() {
        if (pushEvent != null) {
            if (pushEvent.getRepository() != null && pushEvent.getRepository().getUrl() == null) {
                LOGGER.log(Level.WARNING, "No repository url found");
                return;
            }

            if (project instanceof Job<?, ?>) {
                ACL.impersonate(ACL.SYSTEM, new TriggerNotifier(project, secretToken, Jenkins.getAuthentication()) {
                    @Override
                    protected void performOnPost(GitLabPushTrigger trigger) {
                        trigger.onPost(pushEvent);
                    }
                });
                return;
            }
        }
        if (tagPushEvent != null) {
            if (tagPushEvent.getRepository() != null
                    && tagPushEvent.getRepository().getUrl() == null) {
                LOGGER.log(Level.WARNING, "No repository url found");
                return;
            }

            if (project instanceof Job<?, ?>) {
                ACL.impersonate(ACL.SYSTEM, new TriggerNotifier(project, secretToken, Jenkins.getAuthentication()) {
                    @Override
                    protected void performOnPost(GitLabPushTrigger trigger) {
                        trigger.onPost(tagPushEvent);
                    }
                });
                return;
            }
        }
        if (pushSystemHookEvent != null) {
            if (pushSystemHookEvent.getRepository() != null
                    && pushSystemHookEvent.getRepository().getUrl() == null) {
                LOGGER.log(Level.WARNING, "No repository url found");
                return;
            }

            if (project instanceof Job<?, ?>) {
                ACL.impersonate(ACL.SYSTEM, new TriggerNotifier(project, secretToken, Jenkins.getAuthentication()) {
                    @Override
                    protected void performOnPost(GitLabPushTrigger trigger) {
                        trigger.onPost(pushSystemHookEvent);
                    }
                });
                return;
            }
        }
        if (tagPushSystemHookEvent != null) {
            if (tagPushSystemHookEvent.getRepository() != null
                    && tagPushSystemHookEvent.getRepository().getUrl() == null) {
                LOGGER.log(Level.WARNING, "No repository url found");
                return;
            }

            if (project instanceof Job<?, ?>) {
                ACL.impersonate(ACL.SYSTEM, new TriggerNotifier(project, secretToken, Jenkins.getAuthentication()) {
                    @Override
                    protected void performOnPost(GitLabPushTrigger trigger) {
                        trigger.onPost(tagPushSystemHookEvent);
                    }
                });
                return;
            }
        }
        if (project instanceof SCMSourceOwner) {
            ACL.impersonate(ACL.SYSTEM, new SCMSourceOwnerNotifier());
            return;
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
                            if (SCMTrait.find(gitSCMSource.getTraits(), IgnoreOnPushNotificationTrait.class) == null) {
                                LOGGER.log(
                                        Level.FINE,
                                        "Notify scmSourceOwner {0} about changes for {1}",
                                        toArray(project.getName(), gitSCMSource.getRemote()));
                                ((SCMSourceOwner) project).onSCMSourceUpdated(scmSource);
                            } else {
                                LOGGER.log(
                                        Level.FINE,
                                        "Ignore on push notification for scmSourceOwner {0} about changes for {1}",
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
