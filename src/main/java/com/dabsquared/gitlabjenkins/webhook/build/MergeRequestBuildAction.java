package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabMergeRequest;
import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.util.BuildUtil;
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
    private GitLabMergeRequest mergeRequest;

    public MergeRequestBuildAction(AbstractProject<?, ?> project, String json) {
        LOGGER.log(Level.FINE, "MergeRequest: {0}", toPrettyPrint(json));
        this.project = project;
        this.mergeRequest = GitLabMergeRequest.create(json);
    }

    public void execute(StaplerResponse response) {
        String state = mergeRequest.getObjectAttribute().getState();
        if (state.equals("opened") || state.equals("reopened")) {
            if (lastCommitNotYetBuild()) {
                ACL.impersonate(ACL.SYSTEM, new Runnable() {
                    public void run() {
                        GitLabPushTrigger trigger = project.getTrigger(GitLabPushTrigger.class);
                        if (trigger != null) {
                            if (trigger.getCiSkip() && mergeRequest.getObjectAttribute().getDescription().contains("[ci-skip]")) {
                                LOGGER.log(Level.INFO, "Skipping MR " + mergeRequest.getObjectAttribute().getTitle() + " due to ci-skip.");
                                return;
                            }
                            trigger.onPost(mergeRequest);
                        }
                    }
                });
            }
        }
        throw HttpResponses.ok();
    }

    private boolean lastCommitNotYetBuild() {
        if (mergeRequest.getObjectAttribute().getLastCommit() != null) {
            AbstractBuild<?, ?> mergeBuild = BuildUtil.getBuildBySHA1(project, mergeRequest.getObjectAttribute().getLastCommit().getId(), true);
            if (mergeBuild != null && StringUtils.equals(getTargetBranchFromBuild(mergeBuild), mergeRequest.getObjectAttribute().getTargetBranch())) {
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
