package com.dabsquared.gitlabjenkins.webhook.status;

import com.dabsquared.gitlabjenkins.util.BuildUtil;
import hudson.model.AbstractProject;

/**
 * @author Robin Müller
 */
public class BranchStatusPngAction extends StatusPngAction {
    public BranchStatusPngAction(AbstractProject<?, ?> project, String branchName) {
        super(project, BuildUtil.getBuildByBranch(project, branchName));
    }
}
