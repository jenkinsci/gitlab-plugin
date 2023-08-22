package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.gitlab4j.api.webhook.EventProject;
import org.gitlab4j.api.webhook.PipelineEvent;
import org.gitlab4j.api.webhook.PipelineEvent.ObjectAttributes;

/**
 * @author Milena Zachow
 */
public class PipelineBuildAction extends BuildWebHookAction {

    private static final Logger LOGGER = Logger.getLogger(PipelineBuildAction.class.getName());
    private Item project;
    private PipelineEvent pipelineEvent;
    private final String secretToken;

    public PipelineBuildAction(Item project, PipelineEvent pipelineEvent, String secretToken) {
        LOGGER.log(Level.FINE, pipelineEvent.toString());
        this.project = project;
        this.pipelineEvent = pipelineEvent;
        this.secretToken = secretToken;
    }

    void processForCompatibility() {
        // if no project is defined, set it here
        final ObjectAttributes attributes = this.pipelineEvent.getObjectAttributes();
        if (this.pipelineEvent.getProject() == null && attributes != null) {
            final String source = attributes.getSource();
            if (source != null) {
                EventProject project = new EventProject();
                project.setNamespace(source.replaceFirst("/", "").substring(0, source.lastIndexOf("/")));
                this.pipelineEvent.setProject(project);
            } else {
                LOGGER.log(Level.WARNING, "Could not find suitable namespace.");
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
                trigger.onPost(pipelineEvent);
            }
        });
        return;
    }
}
