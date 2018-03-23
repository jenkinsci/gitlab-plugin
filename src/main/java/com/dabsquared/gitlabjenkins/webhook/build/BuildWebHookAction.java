package com.dabsquared.gitlabjenkins.webhook.build;

import java.util.logging.Logger;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.WebHook;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.Permission;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerResponse;
import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import com.dabsquared.gitlabjenkins.webhook.WebHookAction;

import static com.dabsquared.gitlabjenkins.util.TriggerUtil.getFromJob;

/**
 * @author Xinran Xiao
 */
abstract class BuildWebHookAction implements WebHookAction {
    private static final Logger LOGGER = Logger.getLogger(BuildWebHookAction.class.getName());
    private static final String TOKEN_ERROR_MESSAGE = "Unauthorized (Did you forget to add API Token to the web hook?)";

    abstract void processForCompatibility();

    abstract void execute();

    public final void execute(StaplerResponse response) {
        processForCompatibility();
        execute();
    }

    protected static class TriggerAction<T extends WebHook> implements Runnable {
        private final T webHook;
        private final Item project;
        private final String secretToken;
        private final Authentication authentication;

        TriggerAction(T webHook, Item project, String secretToken) {
            this.webHook = webHook;
            this.project = project;
            this.secretToken = secretToken;
            this.authentication = Jenkins.getAuthentication();
        }

        @Override
        public void run() {
            GitLabPushTrigger trigger = getFromJob(project, webHook);
            if (trigger != null) {
                if (StringUtils.isEmpty(trigger.getSecretToken())) {
                    checkPermission(Item.BUILD);
                } else if (!StringUtils.equals(trigger.getSecretToken(), secretToken)) {
                    throw HttpResponses.errorWithoutStack(401, "Invalid token");
                }
                boolean triggerExecuted = trigger.onPost(webHook);
                if (!triggerExecuted) {
                    throw HttpResponses.errorWithoutStack(500, "Trigger not executed");
                }
            } else {
                LOGGER.warning("No trigger found");
                throw HttpResponses.errorWithoutStack(400, "Trigger not found");
            }
        }

        private void checkPermission(Permission permission) {
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null) {
                throw HttpResponses.errorWithoutStack(500, "Jenkins not ready");
            }
            Descriptor descriptor = jenkins.getDescriptor(GitLabConnectionConfig.class);
            if (((GitLabConnectionConfig) descriptor).isUseAuthenticatedEndpoint()) {
                if (!jenkins.getACL().hasPermission(authentication, permission)) {
                    LOGGER.finest(TOKEN_ERROR_MESSAGE);
                    throw HttpResponses.errorWithoutStack(403, TOKEN_ERROR_MESSAGE);
                }
            }
        }
    }
}
