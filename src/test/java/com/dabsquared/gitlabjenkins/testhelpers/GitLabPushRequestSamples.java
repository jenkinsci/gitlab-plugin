package com.dabsquared.gitlabjenkins.testhelpers;

import com.dabsquared.gitlabjenkins.GitLabPushRequest;

public interface GitLabPushRequestSamples {
	GitLabPushRequest pushBrandNewMasterBranchRequest();

	GitLabPushRequest pushNewBranchRequest();

	GitLabPushRequest pushCommitRequest();

	GitLabPushRequest mergePushRequest();

	GitLabPushRequest pushNewTagRequest();

	GitLabPushRequest deleteBranchRequest();
}
