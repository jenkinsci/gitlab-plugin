package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.NoteHook;
import com.dabsquared.gitlabjenkins.util.JsonUtil;
import com.dabsquared.gitlabjenkins.webhook.WebHookAction;
import com.fasterxml.jackson.databind.JsonNode;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerResponse;

import java.util.logging.Level;
import java.util.logging.Logger;
 
import static com.dabsquared.gitlabjenkins.util.JsonUtil.toPrettyPrint;

/**
 * @author Nikolay Ustinov
 */
public class NoteBuildAction implements WebHookAction {

    private final static Logger LOGGER = Logger.getLogger(NoteBuildAction.class.getName());
    private Item project;
    private NoteHook noteHook;
    private final String secretToken;

    public NoteBuildAction(Item project, String json, String secretToken) {
        LOGGER.log(Level.FINE, "Note: {0}", toPrettyPrint(json));
        this.project = project;
        this.noteHook = JsonUtil.read(json, NoteHook.class);
        this.secretToken = secretToken;
    }

    /**
     * Alternative Constructor which takes in an already deserialized Json Tree.
     * @param project Jenkins Project Item
     * @param json Payload Json Tree
     * @param secretToken Secret Token
     */
    public NoteBuildAction(Item project, JsonNode json, String secretToken) {
        this.project = project;
        this.noteHook = JsonUtil.read(json, NoteHook.class);
        this.secretToken = secretToken;
    }

    @Override
    public void execute(StaplerResponse response) {
        if (!(project instanceof Job<?, ?>)) {
            throw HttpResponses.errorWithoutStack(409, "Note Hook is not supported for this project");
        }
        ACL.impersonate(ACL.SYSTEM, new BuildWebHookAction.TriggerNotifier(project, secretToken, Jenkins.getAuthentication()) {
            @Override
            protected void performOnPost(GitLabPushTrigger trigger) {
                trigger.onPost(noteHook);
            }
        });
        throw HttpResponses.ok();
    }

    @Override
    public void executeNoResponse(StaplerResponse response){
        if (!(project instanceof Job<?, ?>)) {
          LOGGER.log(Level.WARNING, "Note Hook is not supported for this project");
          return;
        }
        ACL.impersonate(ACL.SYSTEM, new BuildWebHookAction.TriggerNotifier(project, secretToken, Jenkins.getAuthentication()) {
            @Override
            protected void performOnPost(GitLabPushTrigger trigger) {
                trigger.onPost(noteHook);
            }
        });
    }
}
