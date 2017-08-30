package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.*;
import com.dabsquared.gitlabjenkins.util.JsonUtil;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.util.JsonUtil.toPrettyPrint;

/**
 * @author Milena Zachow
 */
public class PipelineBuildAction extends BuildWebHookAction {

    private final static Logger LOGGER = Logger.getLogger(PipelineBuildAction.class.getName());
    private Item project;
    private PipelineHook pipelineBuildHook;
    private final String secretToken;

    public PipelineBuildAction(Item project, String json, String secretToken) {
        LOGGER.log(Level.FINE, "Pipeline event: {0}", toPrettyPrint(json));
        this.project = project;
        this.pipelineBuildHook = JsonUtil.read(json, PipelineHook.class);
        this.secretToken = secretToken;
    }

    void processForCompatibility() {
        //if no project is defined, set it here
        if (this.pipelineBuildHook.getProject() == null && this.pipelineBuildHook.getRepository() != null) {
            try {
                String path = new URL(this.pipelineBuildHook.getRepository().getGitHttpUrl()).getPath();
                if (StringUtils.isNotBlank(path)) {
                    Project project = new Project();
                    project.setNamespace(path.replaceFirst("/", "").substring(0, path.lastIndexOf("/")));
                    this.pipelineBuildHook.setProject(project);
                } else {
                    LOGGER.log(Level.WARNING, "Could not find suitable namespace.");
                }
            } catch (MalformedURLException ignored) {
                LOGGER.log(Level.WARNING, "Invalid repository url found while building namespace.");
            }
        }
    }

    void execute() {
        if (!(project instanceof Job<?, ?>)) {
            throw HttpResponses.errorWithoutStack(409, "Pipeline Hook is not supported for this project");
        }
        ACL.impersonate(ACL.SYSTEM, new TriggerNotifier(project, secretToken, Jenkins.getAuthentication()) {
            @Override
            protected void performOnPost(GitLabPushTrigger trigger) {
                trigger.onPost(pipelineBuildHook);
            }
        });
        throw HttpResponses.ok();
    }

}

