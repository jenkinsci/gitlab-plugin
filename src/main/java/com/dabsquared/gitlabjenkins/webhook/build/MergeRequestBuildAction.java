package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.model.State;
import com.dabsquared.gitlabjenkins.util.BuildUtil;
import com.dabsquared.gitlabjenkins.util.GsonUtil;
import com.dabsquared.gitlabjenkins.webhook.WebHookAction;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.ParametersAction;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerResponse;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.util.GsonUtil.toPrettyPrint;

/**
 * @author Robin Müller
 */
public class MergeRequestBuildAction implements WebHookAction {

    private final static Logger LOGGER = Logger.getLogger(MergeRequestBuildAction.class.getName());
    private AbstractProject<?, ?> project;
    private MergeRequestHook mergeRequestHook;

    public MergeRequestBuildAction(AbstractProject<?, ?> project, String json) {
        LOGGER.log(Level.FINE, "MergeRequest: {0}", toPrettyPrint(json));
        this.project = project;
        this.mergeRequestHook = GsonUtil.getGson().fromJson(json, MergeRequestHook.class);
    }

    public void execute(StaplerResponse response) {
        State state = mergeRequestHook.getObjectAttributes().getState();
        if (state == State.opened || state == State.reopened) {
            if (lastCommitNotYetBuild()) {
                ACL.impersonate(ACL.SYSTEM, new Runnable() {
                    public void run() {
                        GitLabPushTrigger trigger = project.getTrigger(GitLabPushTrigger.class);
                        if (trigger != null) {
                            if (trigger.getCiSkip() && mergeRequestHook.getObjectAttributes().getDescription().contains("[ci-skip]")) {
                                LOGGER.log(Level.INFO, "Skipping MR " + mergeRequestHook.getObjectAttributes().getTitle() + " due to ci-skip.");
                                return;
                            }
                            trigger.onPost(mergeRequestHook);
                        }
                    }
                });
            }
        }
        throw HttpResponses.ok();
    }

    private boolean lastCommitNotYetBuild() {
        if (mergeRequestHook.getObjectAttributes().getLastCommit() != null) {
            AbstractBuild<?, ?> mergeBuild = BuildUtil.getBuildBySHA1(project, mergeRequestHook.getObjectAttributes().getLastCommit().getId(), true);
            if (mergeBuild != null && StringUtils.equals(getTargetBranchFromBuild(mergeBuild), mergeRequestHook.getObjectAttributes().getTargetBranch())) {
                LOGGER.log(Level.INFO, "Last commit in Merge Request has already been built in build #" + mergeBuild.getNumber());
                return false;
            }
        }
        return true;
    }

    private String getTargetBranchFromBuild(AbstractBuild<?, ?> mergeBuild) {
        return (String) mergeBuild.getAction(ParametersAction.class).getParameter("gitlabTargetBranch").getValue();
    }
}
