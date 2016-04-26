package com.dabsquared.gitlabjenkins.webhook.status;

import com.dabsquared.gitlabjenkins.util.BuildUtil;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
public class BranchBuildPageRedirectAction extends BuildPageRedirectAction {
    public BranchBuildPageRedirectAction(Job<?, ?> project, String branchName) {
        super(BuildUtil.getBuildByBranch(project, branchName));
    }
}
