package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import com.dabsquared.gitlabjenkins.util.JsonUtil;
import com.dabsquared.gitlabjenkins.webhook.WebHookAction;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerResponse;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.util.JsonUtil.toPrettyPrint;

/**
 * @author Robin Müller
 */
public class PushBuildAction implements WebHookAction {

    private final static Logger LOGGER = Logger.getLogger(PushBuildAction.class.getName());
    private final Job<?, ?> project;

    private PushHook pushHook;

    public PushBuildAction(Job<?, ?> project, String json) {
        LOGGER.log(Level.FINE, "Push: {0}", toPrettyPrint(json));
        this.project = project;
        this.pushHook = processForCompatibility(JsonUtil.read(json, PushHook.class));
    }

    public void execute(StaplerResponse response) {
        if (pushHook.getRepository() != null && pushHook.getRepository().getUrl() == null) {
            LOGGER.log(Level.WARNING, "No repository url found.");
            return;
        }

        ACL.impersonate(ACL.SYSTEM, new Runnable() {
            public void run() {
                GitLabPushTrigger trigger = GitLabPushTrigger.getFromJob(project);
                if (trigger != null) {
                    trigger.onPost(pushHook);
                }
            }
        });
        throw HttpResponses.ok();
    }

    private static PushHook processForCompatibility(PushHook pushHook) {
        // Fill in project if it's not defined.
        if (pushHook.getProject() == null) {
            if (pushHook.getRepository() != null) {
                try {
                    String path = new URL(pushHook.getRepository().getGitHttpUrl()).getPath();
                    if (StringUtils.isNotBlank(path)) {
                        Project project = new Project();
                        project.setNamespace(path.replaceFirst("/", "").substring(0, path.lastIndexOf("/")));
                        pushHook.setProject(project);
                    } else {
                        LOGGER.log(Level.WARNING, "Could not find suitable namespace.");
                    }
                } catch (MalformedURLException ignored) {
                    LOGGER.log(Level.WARNING, "Invalid repository url found while building namespace.");
                }
            }
        }

        return pushHook;
    }
}
