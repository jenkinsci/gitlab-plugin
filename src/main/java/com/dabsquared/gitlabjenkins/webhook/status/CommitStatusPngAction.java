package com.dabsquared.gitlabjenkins.webhook.status;

import com.dabsquared.gitlabjenkins.util.BuildUtil;
import hudson.model.AbstractProject;

/**
 * @author Robin Müller
 */
public class CommitStatusPngAction extends StatusPngAction {
    public CommitStatusPngAction(AbstractProject<?, ?> project, String sha1) {
        super(project, BuildUtil.getBuildBySHA1(project, sha1, false));
    }
}
