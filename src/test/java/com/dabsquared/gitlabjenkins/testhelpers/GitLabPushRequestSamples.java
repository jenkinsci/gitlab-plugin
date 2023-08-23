package com.dabsquared.gitlabjenkins.testhelpers;

import org.gitlab4j.api.webhook.PushEvent;

public interface GitLabPushRequestSamples {
    PushEvent pushBrandNewMasterBranchRequest();

    PushEvent pushNewBranchRequest();

    PushEvent pushCommitRequest();

    PushEvent mergePushRequest();

    PushEvent pushNewTagRequest();

    PushEvent deleteBranchRequest();
}
