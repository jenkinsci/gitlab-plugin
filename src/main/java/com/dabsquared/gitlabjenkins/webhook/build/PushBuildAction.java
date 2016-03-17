package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.model.PushHook;
import com.dabsquared.gitlabjenkins.util.GsonUtil;
import com.dabsquared.gitlabjenkins.webhook.WebHookAction;
import hudson.model.AbstractProject;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabCommit;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;
import org.kohsuke.stapler.StaplerResponse;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.model.Repository.nullRepository;
import static com.dabsquared.gitlabjenkins.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.model.builder.generated.MergeRequestHookBuilder.mergeRequestHook;
import static com.dabsquared.gitlabjenkins.model.builder.generated.ObjectAttributesBuilder.objectAttributes;
import static com.dabsquared.gitlabjenkins.util.GsonUtil.toPrettyPrint;

/**
 * @author Robin Müller
 */
public class PushBuildAction implements WebHookAction {

    private final static Logger LOGGER = Logger.getLogger(PushBuildAction.class.getName());
    private final AbstractProject<?, ?> project;

    private PushHook pushHook;

    public PushBuildAction(AbstractProject<?, ?> project, String json) {
        LOGGER.log(Level.FINE, "Push: {0}", toPrettyPrint(json));
        this.project = project;
        this.pushHook = GsonUtil.getGson().fromJson(json, PushHook.class);
    }

    public void execute(StaplerResponse response) {
        String repositoryUrl = pushHook.getRepository().optUrl().orNull();
        if (repositoryUrl == null) {
            LOGGER.log(Level.WARNING, "No repository url found.");
            return;
        }

        ACL.impersonate(ACL.SYSTEM, new Runnable() {
            public void run() {
                GitLabPushTrigger trigger = project.getTrigger(GitLabPushTrigger.class);
                if (trigger != null) {
                    trigger.onPost(pushHook);
                }
            }
        });
        throw HttpResponses.ok();
    }
}
