package com.dabsquared.gitlabjenkins.util;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.Branch;
import hudson.plugins.git.util.BuildData;
import hudson.plugins.git.util.MergeRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Robin Müller
 */
public class BuildUtil {
    
    public static Run<?, ?> getBuildByBranch(Job<?, ?> project, String branchName) {
        for (final Run<?, ?> build : project.getBuilds()) {
            final BuildData data = build.getAction(BuildData.class);
            final MergeRecord merge = build.getAction(MergeRecord.class);
            final List<String> potentialBranchNames = new ArrayList<>();
            if (hasLastBuildFromGitData(data) && isNoMergeBuild(data, merge)) {
                getBranchNamesFromGitData(data, potentialBranchNames);
            } else {
                potentialBranchNames.addAll(getBranchNamesFromHook(build));
            }

            if (branchFound(branchName, potentialBranchNames)) {
                return build;
            }
        }
        return null;
    }

    private static boolean branchFound(final String branchName, final List<String> branchNames) {
        for (final String potentialBranchName : branchNames) {
            if (potentialBranchName.endsWith("/" + branchName)) {
                return true;
            }
        }
        return false;
    }

    private static void getBranchNamesFromGitData(final BuildData data, final List<String> branchNames) {
        for (final Branch branch : data.lastBuild.getRevision().getBranches()) {
            branchNames.add(branch.getName());
        }
    }

    private static List<String> getBranchNamesFromHook(final Run<?, ?> build) {
        final List<String> branchNames = new ArrayList<>();
        final GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);
        if (cause != null) {
            branchNames.add(cause.getData().getBranch());
            branchNames.add(cause.getData().getSourceBranch());
            branchNames.add(cause.getData().getTargetBranch());
        }
        return branchNames;
    }

    public static Run<?, ?> getBuildBySHA1WithoutMergeBuilds(Job<?, ?> project, String sha1) {
        for (Run<?, ?> build : project.getBuilds()) {
            BuildData data = build.getAction(BuildData.class);
            MergeRecord merge = build.getAction(MergeRecord.class);
            if (isNoMergeBuild(data, merge) && revisionFound(sha1, build, data)) {
                return build;
            }
        }
        return null;
    }

    private static boolean revisionFound(final String sha1, final Run<?, ?> build, final BuildData data) {
        return (hasLastBuildFromGitData(data) && data.lastBuild.isFor(sha1))|| revisionFoundFromHookData(sha1, build);
    }

    public static Run<?, ?> getBuildBySHA1IncludingMergeBuilds(Job<?, ?> project, String sha1) {
        for (Run<?, ?> build : project.getBuilds()) {
            if (revisionFoundFromGitData(sha1, build)) {
                return build;
            }

            //also try to retrieve builds via the hook to allow for jobs to work without a Git setup
            if (revisionFoundFromHookData(sha1, build)) {
                return build;
            }
        }
        return null;
    }

    private static boolean revisionFoundFromHookData(final String sha1, final Run<?, ?> build) {
        final GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);
        if (cause != null) {
            final String lastCommit = cause.getData().getLastCommit();

            if (lastCommit != null && lastCommit.equals(sha1)) {
                return true;
            }
        }
        return false;
    }

    private static boolean revisionFoundFromGitData(final String sha1, final Run<?, ?> build) {
        //slight difference - this uses the marked revision, which according to Jenkins docs might differ slightly from revision used above (due to merges etc)
        final BuildData data = build.getAction(BuildData.class);
        return data != null
            && data.lastBuild != null
            && data.lastBuild.getMarked() != null
            && data.lastBuild.getMarked().getSha1String().equals(sha1);
    }

    private static boolean isNoMergeBuild(BuildData data, MergeRecord merge) {
        return merge == null || merge.getSha1().equals(data.lastBuild.getMarked().getSha1String());
    }
    
    private static boolean hasLastBuildFromGitData(BuildData data) {
        return data != null && data.lastBuild != null && data.lastBuild.getRevision() != null;
    }
}
