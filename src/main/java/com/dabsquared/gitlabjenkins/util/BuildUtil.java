package com.dabsquared.gitlabjenkins.util;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import hudson.model.Job;
import hudson.model.Run;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Robin Müller
 */
public class BuildUtil {
    
    public static Run<?, ?> getBuildByBranch(Job<?, ?> project, String branchName) {
        for (final Run<?, ?> build : project.getBuilds()) {
            final List<String> potentialBranchNames = new ArrayList<>();
            potentialBranchNames.addAll(getBranchNamesFromHook(build));

            if (branchFound(branchName, potentialBranchNames)) {
                return build;
            }
        }
        return null;
    }

    private static boolean branchFound(final String branchName, final List<String> branchNames) {
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

    public static Run<?, ?> getBuildBySHA1(Job<?, ?> project, String sha1) {
        for (Run<?, ?> build : project.getBuilds()) {
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


}
