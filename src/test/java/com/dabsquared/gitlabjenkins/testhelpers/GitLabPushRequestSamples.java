package com.dabsquared.gitlabjenkins.testhelpers;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;

public interface GitLabPushRequestSamples {
	PushHook pushBrandNewMasterBranchRequest();

	PushHook pushNewBranchRequest();

	PushHook pushCommitRequest();

	PushHook mergePushRequest();

	PushHook pushNewTagRequest();

	PushHook deleteBranchRequest();
}
