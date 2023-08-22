package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.gitlab4j.api.systemhooks.MergeRequestSystemHookEvent;
import org.gitlab4j.api.webhook.EventProject;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.gitlab4j.api.webhook.MergeRequestEvent.ObjectAttributes;

/**
 * @author Robin Müller
 */
public class MergeRequestBuildAction extends BuildWebHookAction {

    private static final Logger LOGGER = Logger.getLogger(MergeRequestBuildAction.class.getName());
    private Item project;
    private MergeRequestEvent mergeRequestEvent;
    private MergeRequestSystemHookEvent mergeRequestSystemHookEvent;
    private final String secretToken;

    public MergeRequestBuildAction(Item project, MergeRequestEvent mergeRequestEvent, String secretToken) {
        LOGGER.log(Level.FINE, mergeRequestEvent.toString());
        this.project = project;
        this.mergeRequestEvent = mergeRequestEvent;
        this.secretToken = secretToken;
    }

    public MergeRequestBuildAction(
            Item project, MergeRequestSystemHookEvent mergeRequestSystemHookEvent, String secretToken) {
        this.project = project;
        this.mergeRequestSystemHookEvent = mergeRequestSystemHookEvent;
        this.secretToken = secretToken;
    }

    void processForCompatibility() {
        // url and homepage are introduced in 8.x versions of Gitlab
        if (mergeRequestEvent != null) {
            final ObjectAttributes attributes = this.mergeRequestEvent.getObjectAttributes();
            if (attributes != null) {
                final EventProject source = attributes.getSource();
                if (source != null && source.getHttpUrl() != null) {
                    if (source.getUrl() == null) {
                        source.setUrl(source.getHttpUrl());
                    }
                    if (source.getHomepage() == null) {
                        source.setHomepage(source.getHttpUrl()
                                .substring(0, source.getHttpUrl().lastIndexOf(".git")));
                    }
                }

                // The MergeRequestHookTriggerHandlerImpl is looking for Project
                if (mergeRequestEvent.getProject() == null && attributes.getTarget() != null) {
                    mergeRequestEvent.setProject(attributes.getTarget());
                }
            }
        } else {
            final ObjectAttributes attributes = this.mergeRequestSystemHookEvent.getObjectAttributes();
            if (attributes != null) {
                final EventProject source = attributes.getSource();
                if (source != null && source.getHttpUrl() != null) {
                    if (source.getWebUrl() == null) {
                        source.setWebUrl(source.getHttpUrl());
                    }
                    if (source.getHomepage() == null) {
                        source.setHomepage(source.getHttpUrl()
                                .substring(0, source.getHttpUrl().lastIndexOf(".git")));
                    }
                }

                // The MergeRequestHookTriggerHandlerImpl is looking for Project
                if (mergeRequestSystemHookEvent.getProject() == null && attributes.getTarget() != null) {
                    mergeRequestSystemHookEvent.setProject(attributes.getTarget());
                }
            }
        }
    }

    public void execute() {
        if (!(project instanceof Job<?, ?>)) {
            throw HttpResponses.errorWithoutStack(409, "Merge Request Hook is not supported for this project");
        }

        if (mergeRequestEvent != null) {
            ACL.impersonate(ACL.SYSTEM, new TriggerNotifier(project, secretToken, Jenkins.getAuthentication()) {
                @Override
                protected void performOnPost(GitLabPushTrigger trigger) {
                    trigger.onPost(mergeRequestEvent);
                }
            });
        } else {
            ACL.impersonate(ACL.SYSTEM, new TriggerNotifier(project, secretToken, Jenkins.getAuthentication()) {
                @Override
                protected void performOnPost(GitLabPushTrigger trigger) {
                    trigger.onPost(mergeRequestSystemHookEvent);
                }
            });
        }
        return;
    }
}
