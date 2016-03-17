package com.dabsquared.gitlabjenkins.util;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.Branch;
import hudson.plugins.git.util.BuildData;
import hudson.plugins.git.util.MergeRecord;

/**
 * @author Robin Müller
 */
public class BuildUtil {
    public static AbstractBuild<?, ?> getBuildByBranch(AbstractProject<?, ?> project, String branchName) {
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

    public static Run<?, ?> getBuildBySHA1(Job<?, ?> project, String sha1, boolean includeMergeBuilds) {
        for (Run<?, ?> build : project.getBuilds()) {
            BuildData data = build.getAction(BuildData.class);
            MergeRecord merge = build.getAction(MergeRecord.class);
            if (hasLastBuild(data) && (isNoMergeBuild(data, merge) || includeMergeBuilds)) {
                if (data.lastBuild.isFor(sha1)) {
                    return build;
                }
            }
        }
        return null;
    }

    private static boolean isNoMergeBuild(BuildData data, MergeRecord merge) {
        return merge == null || merge.getSha1().equals(data.lastBuild.getMarked().getSha1String());
    }

    private static boolean hasLastBuild(BuildData data) {
        return data != null && data.lastBuild != null && data.lastBuild.getRevision() != null;
    }
}
