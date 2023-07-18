package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.webhook.WebHookAction;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.gitlab4j.api.webhook.NoteEvent;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author Nikolay Ustinov
 */
public class NoteBuildAction implements WebHookAction {

    public static final Logger LOGGER = Logger.getLogger(NoteBuildAction.class.getName());

    private Item project;
    private NoteEvent noteEvent;
    private final String secretToken;

    public NoteBuildAction(Item project, NoteEvent noteEvent, String secretToken) {
        LOGGER.log(Level.FINE, noteEvent.toString());
        this.project = project;
        this.noteEvent = noteEvent;
        this.secretToken = secretToken;
    }

    public void execute(StaplerResponse response) {
        if (!(project instanceof Job<?, ?>)) {
            throw HttpResponses.errorWithoutStack(409, "Note Hook is not supported for this project");
        }
        ACL.impersonate(
                ACL.SYSTEM, new BuildWebHookAction.TriggerNotifier(project, secretToken, Jenkins.getAuthentication()) {
                    @Override
                    protected void performOnPost(GitLabPushTrigger trigger) {
                        trigger.onPost(noteEvent);
                    }
                });
        return;
    }
}
