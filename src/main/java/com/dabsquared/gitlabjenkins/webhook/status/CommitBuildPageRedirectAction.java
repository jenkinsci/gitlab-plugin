package com.dabsquared.gitlabjenkins.webhook.status;

import com.dabsquared.gitlabjenkins.util.BuildUtil;
import hudson.model.AbstractProject;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
public class CommitBuildPageRedirectAction extends BuildPageRedirectAction {
    public CommitBuildPageRedirectAction(Job<?, ?> project, String sha1) {
        super(BuildUtil.getBuildBySHA1(project, sha1, true));
    }
}
