package com.dabsquared.gitlabjenkins.util;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.Branch;
import hudson.plugins.git.util.BuildData;
import hudson.plugins.git.util.MergeRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Robin Müller
 */
public class BuildUtil {
    
    public static Run<?, ?> getBuildByBranch(Job<?, ?> project, String branchName) {
        for (final Run<?, ?> build : project.getBuilds()) {
            final List<String> potentialBranchNames = getBranchNames(build);
            if (branchFound(branchName, potentialBranchNames)) {
                return build;
            }
        }
        return null;
    }

    private static boolean branchFound(final String branchName, final Iterable<String> branchNames) {
        final String prefix = getBranchPrefix(branchName);
        for (final String potentialBranchName : branchNames) {
            if (potentialBranchName.endsWith(prefix + branchName)) {
                return true;
            }
        }
        return false;
    }

    private static String getBranchPrefix(String potentialBranchName) {
        //why this odd workaround? if the branch name is located in a folder, we dont want to accidentally match anything else
        //but we have to accept that some branches (eg. master) will not be located in a folder
        final String prefix;
        if (potentialBranchName.contains("/")) {
            prefix = "/";
        } else {
            prefix = "";
        }
        return prefix;
    }

    private static List<String> getBranchNames(final Run<?, ?> build) {
        final GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);
        if (cause != null) {
            final List<String> branchNames = new ArrayList<>();
            branchNames.add(cause.getData().getBranch());
            branchNames.add(cause.getData().getSourceBranch());
            branchNames.add(cause.getData().getTargetBranch());
            return branchNames;
        }
        //fallback to scm
        return getBranchNamesFromSCM(build);
    }
    
    private static boolean isNoMergeBuild(BuildData data, MergeRecord merge) {
        return merge == null || merge.getSha1().equals(data.lastBuild.getMarked().getSha1String());
    }

    private static boolean hasLastBuild(BuildData data) {
        return data != null && data.lastBuild != null && data.lastBuild.getRevision() != null;
    }
    
    private static List<String> getBranchNamesFromSCM(final Run<?, ?> build) {
        BuildData data = build.getAction(BuildData.class);
        MergeRecord merge = build.getAction(MergeRecord.class);
        if (hasLastBuild(data) && isNoMergeBuild(data, merge)) {
            ArrayList<String> branchNames = new ArrayList<>();
            for (Branch branch : data.lastBuild.getRevision().getBranches()) {
                branchNames.add(branch.getName());
            }
            return branchNames;
        }
        return Collections.emptyList();
    }

    public static Run<?, ?> getBuildBySHA1IncludingMergeBuilds(Job<?, ?> project, String sha1) {
        for (Run<?, ?> build : project.getBuilds()) {
            if (hasSHA1RevisionIncludeMergeBuilds(build, sha1)) {
                return build;
            }
        }
        return null;
    }

    private static boolean hasSHA1RevisionIncludeMergeBuilds(final Run<?, ?> build, final String sha1) {
        final GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);
        if (cause != null) {
            final String lastCommit = cause.getData().getLastCommit();
            if (lastCommit != null && lastCommit.equals(sha1)) {
                return true;
            }
        }
        return hasSHA1RevisionInBuildData(build, sha1);
    }
    
    private static boolean hasSHA1RevisionInBuildData(final Run<?, ?> build, final String sha1) {
        BuildData data = build.getAction(BuildData.class);
        if (data != null
            && data.lastBuild != null
            && data.lastBuild.getMarked() != null
            && data.lastBuild.getMarked().getSha1String().equals(sha1)) {
            return true;
        }
        return false;
    }
    
    
    public static Run<?, ?> getBuildBySHA1WithoutMergeBuilds(Job<?, ?> project, String sha1) {
        for (Run<?, ?> build : project.getBuilds()) {
            if (hasSHA1WithoutMergeBuilds(sha1, build)) {
                return build;
            }
        }
        return null;
    }

    private static boolean hasSHA1WithoutMergeBuilds(String sha1, Run<?, ?> build) {
        final GitLabWebHookCause cause = build.getCause(GitLabWebHookCause.class);
        if (cause != null) {
            if (!cause.getData().isMergeBuild())
            {
                return false;
            }
            final String lastCommit = cause.getData().getLastCommit();
            if (lastCommit != null && lastCommit.equals(sha1)) {
                return true;
            }
        }
        BuildData data = build.getAction(BuildData.class);
        MergeRecord merge = build.getAction(MergeRecord.class);
        if (hasLastBuild(data) && isNoMergeBuild(data, merge) && data.lastBuild.isFor(sha1)) {
            return true;
        }
        return false;
    }


}
