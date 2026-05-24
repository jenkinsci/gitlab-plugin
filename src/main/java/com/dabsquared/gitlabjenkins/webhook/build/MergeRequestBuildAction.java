package com.dabsquared.gitlabjenkins.webhook.build;

import static com.dabsquared.gitlabjenkins.util.JsonUtil.toPrettyPrint;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestObjectAttributes;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.Project;
import com.dabsquared.gitlabjenkins.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

/**
 * @author Robin Müller
 */
public class MergeRequestBuildAction extends BuildWebHookAction {

    private static final Logger LOGGER = Logger.getLogger(MergeRequestBuildAction.class.getName());
    private Item project;
    private MergeRequestHook mergeRequestHook;
    private final String secretToken;

    public MergeRequestBuildAction(Item project, String json, String secretToken) {
        LOGGER.log(Level.FINE, "MergeRequest: {0}", toPrettyPrint(json));
        this.project = project;
        this.mergeRequestHook = JsonUtil.read(json, MergeRequestHook.class);
        this.secretToken = secretToken;
    }

    /**
     * Alternative Constructor which takes in an already deserialized Json Tree.
     * @param project Jenkins Project Item
     * @param json Payload Json Tree
     * @param secretToken Secret Token
     */
    public MergeRequestBuildAction(Item project, JsonNode json, String secretToken) {
        LOGGER.log(Level.FINE, "MergeRequest: {0}", toPrettyPrint(json));
        this.project = project;
        this.mergeRequestHook = JsonUtil.read(json, MergeRequestHook.class);
        this.secretToken = secretToken;
    }

    void processForCompatibility() {
        // url and homepage are introduced in 8.x versions of Gitlab
        final MergeRequestObjectAttributes attributes = this.mergeRequestHook.getObjectAttributes();
        if (attributes != null) {
            final Project source = attributes.getSource();
            if (source != null && source.getHttpUrl() != null) {
                if (source.getUrl() == null) {
                    source.setUrl(source.getHttpUrl());
                }
                if (source.getHomepage() == null) {
                    source.setHomepage(
                            source.getHttpUrl().substring(0, source.getHttpUrl().lastIndexOf(".git")));
                }
            }

            // The MergeRequestHookTriggerHandlerImpl is looking for Project
            if (mergeRequestHook.getProject() == null && attributes.getTarget() != null) {
                mergeRequestHook.setProject(attributes.getTarget());
            }
        }
    }

    public void execute() {
        if (!(project instanceof Job<?, ?>)) {
            throw HttpResponses.errorWithoutStack(409, "Merge Request Hook is not supported for this project");
        }
        ACL.impersonate(ACL.SYSTEM, new TriggerNotifier(project, secretToken, Jenkins.getAuthentication()) {
            @Override
            protected void performOnPost(GitLabPushTrigger trigger) {
                trigger.onPost(mergeRequestHook);
            }
        });
        throw HttpResponses.ok();
    }
}
