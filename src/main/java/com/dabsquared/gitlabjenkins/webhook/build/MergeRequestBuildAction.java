package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestObjectAttributes;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.Project;
import com.dabsquared.gitlabjenkins.util.JsonUtil;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.util.HttpResponses;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.util.JsonUtil.toPrettyPrint;

/**
 * @author Robin Müller
 */
public class MergeRequestBuildAction extends BuildWebHookAction {

    private final static Logger LOGGER = Logger.getLogger(MergeRequestBuildAction.class.getName());
    private Item project;
    private MergeRequestHook mergeRequestHook;

    public MergeRequestBuildAction(Item project, String json) {
        LOGGER.log(Level.FINE, "MergeRequest: {0}", toPrettyPrint(json));
        this.project = project;
        this.mergeRequestHook = JsonUtil.read(json, MergeRequestHook.class);
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
                    source.setHomepage(source.getHttpUrl().substring(0, source.getHttpUrl().lastIndexOf(".git")));
                }
            }
        }
    }

    public void execute() {
        if (!(project instanceof Job<?, ?>)) {
            throw HttpResponses.errorWithoutStack(409, "Merge Request Hook is not supported for this project");
        }
        ACL.impersonate(ACL.SYSTEM, new Runnable() {
            public void run() {
                GitLabPushTrigger trigger = GitLabPushTrigger.getFromJob((Job<?, ?>) project);
                if (trigger != null) {
                    trigger.onPost(mergeRequestHook);
                }
            }
        });
        throw HttpResponses.ok();
    }
}
