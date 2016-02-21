package com.dabsquared.gitlabjenkins.webhook;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.git.Branch;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import hudson.plugins.git.util.MergeRecord;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.kohsuke.stapler.StaplerResponse;

import java.io.File;
import java.net.URL;

/**
 * @author Robin Müller
 */
public class StatusPngAction extends BuildStatusAction {


    private final String commitSHA1;
    private final String branchName;

    public StatusPngAction(AbstractProject<?, ?> project, String commitSHA1, String branchName) {
        super(project);
        this.commitSHA1 = commitSHA1;
        this.branchName = branchName;
    }

    @Override
    protected void writeStatusBody(StaplerResponse response, AbstractBuild<?, ?> build, BuildStatus status) {
        Authentication old = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
        try {
            URL resourceUrl = new URL(Jenkins.getInstance().getPlugin("gitlab-plugin").getWrapper().baseResourceURL + getStatusImageUrl(status));
            response.setHeader("Expires","Fri, 01 Jan 1984 00:00:00 GMT");
            response.setHeader("Cache-Control", "no-cache, private");
            response.setHeader("Content-Type", "image/png");
            hudson.util.IOUtils.copy(new File(resourceUrl.toURI()), response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            throw HttpResponses.error(500, "Could not generate response.");
        } finally {
            SecurityContextHolder.getContext().setAuthentication(old);
        }
    }

    @Override
    protected AbstractBuild<?, ?> retrieveBuild(AbstractProject<?, ?> project) {
        if (branchName != null) {
            return getBuildByBranch(project, branchName);
        } else {
            return getBuildBySHA1(project, commitSHA1);
        }
    }

    private AbstractBuild<?, ?> getBuildByBranch(AbstractProject<?, ?> project, String branchName) {
        for (AbstractBuild<?, ?> build : project.getBuilds()) {
            BuildData data = build.getAction(BuildData.class);
            MergeRecord merge = build.getAction(MergeRecord.class);
            if (hasLastBuild(data) && isNoMergeBuild(data, merge)) {
                for (Branch branch : data.lastBuild.getRevision().getBranches()) {
                    if (branch.getName().endsWith("/" + branchName)) {
                        return build;
                    }
                }
            }
        }
        return null;
    }

    private AbstractBuild<?, ?> getBuildBySHA1(AbstractProject<?, ?> project, String sha1) {
        for (AbstractBuild build : project.getBuilds()) {
            BuildData data = build.getAction(BuildData.class);
            MergeRecord merge = build.getAction(MergeRecord.class);
            if (hasLastBuild(data) && isNoMergeBuild(data, merge)) {
                if (data.lastBuild.isFor(sha1)) {
                    return build;
                }
            }
        }
        return null;
    }

    private boolean isNoMergeBuild(BuildData data, MergeRecord merge) {
        return merge == null || merge.getSha1().equals(data.lastBuild.getMarked().getSha1String());
    }

    private boolean hasLastBuild(BuildData data) {
        return data != null && data.lastBuild != null && data.lastBuild.getRevision() != null;
    }

    private String getStatusImageUrl(BuildStatus status) {
        if(status == BuildStatus.RUNNING) {
            return "images/running.png";
        } else if (status == BuildStatus.SUCCESS) {
            return "images/success.png";
        } else if (status == BuildStatus.FAILED) {
            return "images/failed.png";
        } else if (status == BuildStatus.UNSTABLE) {
            return "images/unstable.png";
        } else {
            return "images/unknown.png";
        }
    }
}
