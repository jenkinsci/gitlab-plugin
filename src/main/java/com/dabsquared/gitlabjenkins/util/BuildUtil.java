package com.dabsquared.gitlabjenkins.util;

import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.Branch;
import hudson.plugins.git.util.BuildData;
import hudson.plugins.git.util.MergeRecord;

/**
 * @author Robin Müller
 */
public class BuildUtil {
    public static Run<?, ?> getBuildByBranch(Job<?, ?> project, String branchName) {
        for (Run<?, ?> build : project.getBuilds()) {
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

    public static Run<?, ?> getBuildBySHA1WithoutMergeBuilds(Job<?, ?> project, String sha1) {
        for (Run<?, ?> build : project.getBuilds()) {
            MergeRecord merge = build.getAction(MergeRecord.class);
            for(BuildData data : build.getActions(BuildData.class)) {
                if (hasLastBuild(data) && isNoMergeBuild(data, merge) && data.lastBuild.isFor(sha1)) {
                    return build;
                }
            }
        }
        return null;
    }

    public static Run<?, ?> getBuildBySHA1IncludingMergeBuilds(Job<?, ?> project, String sha1) {
        for (Run<?, ?> build : project.getBuilds()) {
            for(BuildData data : build.getActions(BuildData.class)) {
                if (data != null
                    && data.lastBuild != null
                    && data.lastBuild.getMarked() != null
                    && data.lastBuild.getMarked().getSha1String().equals(sha1)) {
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
