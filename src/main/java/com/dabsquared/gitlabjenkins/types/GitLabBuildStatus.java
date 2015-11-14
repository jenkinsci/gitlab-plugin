package com.dabsquared.gitlabjenkins.types;

import hudson.model.Result;

public enum GitLabBuildStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELED,

    UNKNOWN;

    public String value() {
        return name().toLowerCase();
    }

    public static GitLabBuildStatus valueOf(Result result) {
        if (result.equals(Result.SUCCESS)) {
            return SUCCESS;
        }
        if (result.equals(Result.ABORTED)) {
            return CANCELED;
        }
        if (result.equals(Result.NOT_BUILT)) {
            return PENDING;
        }

        return FAILED;
    }
}
