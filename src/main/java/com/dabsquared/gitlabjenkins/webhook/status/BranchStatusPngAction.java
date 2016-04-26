package com.dabsquared.gitlabjenkins.webhook.status;

import com.dabsquared.gitlabjenkins.util.BuildUtil;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
public class BranchStatusPngAction extends StatusPngAction {
    public BranchStatusPngAction(Job<?, ?> project, String branchName) {
        super(project, BuildUtil.getBuildByBranch(project, branchName));
    }
}
