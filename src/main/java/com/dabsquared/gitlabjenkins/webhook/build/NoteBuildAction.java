package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.NoteHook;
import com.dabsquared.gitlabjenkins.util.JsonUtil;
import com.dabsquared.gitlabjenkins.webhook.WebHookAction;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import org.kohsuke.stapler.StaplerResponse;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.util.JsonUtil.toPrettyPrint;

/**
 * @author Nikolay Ustinov
 */
public class NoteBuildAction implements WebHookAction {

    private final static Logger LOGGER = Logger.getLogger(NoteBuildAction.class.getName());
    private Job<?, ?> project;
    private NoteHook noteHook;

    public NoteBuildAction(Job<?, ?> project, String json) {
        LOGGER.log(Level.FINE, "Note: {0}", toPrettyPrint(json));
        this.project = project;
        this.noteHook = JsonUtil.read(json, NoteHook.class);
    }

    public void execute(StaplerResponse response) {
        ACL.impersonate(ACL.SYSTEM, new Runnable() {
            public void run() {
                GitLabPushTrigger trigger = GitLabPushTrigger.getFromJob(project);
                if (trigger != null) {
                    trigger.onPost(noteHook);
                }
            }
        });
        throw HttpResponses.ok();
    }
}
