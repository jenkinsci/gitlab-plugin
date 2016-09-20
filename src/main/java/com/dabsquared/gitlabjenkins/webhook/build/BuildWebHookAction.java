package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import com.dabsquared.gitlabjenkins.webhook.WebHookAction;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.AccessDeniedException2;
import hudson.security.Permission;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author Xinran Xiao
 */
abstract class BuildWebHookAction implements WebHookAction {
    abstract void processForCompatibility();

    abstract void execute();

    public final void execute(StaplerResponse response) {
        processForCompatibility();
        execute();
    }

    protected abstract static class TriggerNotifier implements Runnable {

        private final Item project;
        private final String secretToken;

        public TriggerNotifier(Item project, String secretToken) {
            this.project = project;
            this.secretToken = secretToken;
        }

        public void run() {
            GitLabPushTrigger trigger = GitLabPushTrigger.getFromJob((Job<?, ?>) project);
            if (trigger != null) {
                if (StringUtils.isEmpty(trigger.getSecretToken())) {
                    checkPermission(Item.BUILD);
                } else if (!StringUtils.equals(trigger.getSecretToken(), secretToken)) {
                    throw HttpResponses.errorWithoutStack(401, "Invalid token");
                }
                performOnPost(trigger);
            }
        }

        private void checkPermission(Permission permission) {
            if (((GitLabConnectionConfig) Jenkins.getInstance().getDescriptor(GitLabConnectionConfig.class)).isUseAuthenticatedEndpoint()) {
                try {
                    Jenkins.getInstance().checkPermission(permission);
                } catch (AccessDeniedException2 e) {
                    throw HttpResponses.errorWithoutStack(403, e.getMessage());
                }
            }
        }

        protected abstract void performOnPost(GitLabPushTrigger trigger);
    }
}
