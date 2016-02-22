package com.dabsquared.gitlabjenkins.webhook.status;

import com.dabsquared.gitlabjenkins.util.BuildUtil;
import hudson.model.AbstractProject;

/**
 * @author Robin Müller
 */
public class BranchBuildPageRedirectAction extends BuildPageRedirectAction {
    public BranchBuildPageRedirectAction(AbstractProject<?, ?> project, String branchName) {
        super(BuildUtil.getBuildByBranch(project, branchName));
    }
}
