package com.dabsquared.gitlabjenkins.testhelpers;

import com.dabsquared.gitlabjenkins.models.request.GitLabPushRequest;

import static com.dabsquared.gitlabjenkins.handlers.GitlabRequestHandler.NO_COMMIT_HASH;

public class GitLabPushRequestSamples_8_1_2_8c8af7b implements GitLabPushRequestSamples {

	private static final String COMMIT_25 = "258d6f6e21e6dda343f6e9f8e78c38f12bb81c87";
	private static final String COMMIT_63 = "63b30060be89f0338123f2d8975588e7d40a1874";
	private static final String COMMIT_64 = "64ed77c360ee7ac900c68292775bee2184c1e593";
	private static final String COMMIT_74 = "742d8d0b4b16792c38c6798b28ba1fa754da165e";
	private static final String COMMIT_E5 = "e5a46665b80965724b45fe921788105258b3ec5c";

	public GitLabPushRequest pushBrandNewMasterBranchRequest() {
		GitLabPushRequest pushRequest = new GitLabPushRequestBuilder().withRef("refs/heads/master").withBefore(NO_COMMIT_HASH)
				.withAfter(COMMIT_63).withCheckoutSha(COMMIT_63)
				// no commit on new branches
				.build();
		return pushRequest;
	}

	public GitLabPushRequest pushNewBranchRequest() {
		GitLabPushRequest pushRequest = new GitLabPushRequestBuilder().withRef("refs/heads/test-new-branch1")
				.withBefore(NO_COMMIT_HASH).withAfter(COMMIT_25).withCheckoutSha(COMMIT_25)
				// no commit on new branches
				.build();
		return pushRequest;
	}

	public GitLabPushRequest pushCommitRequest() {
		GitLabPushRequest pushRequest = new GitLabPushRequestBuilder().withRef("refs/heads/test-new-branch1")
				.withBefore(COMMIT_25).withAfter(COMMIT_74).withCheckoutSha(COMMIT_74).addCommit(COMMIT_74).build();

		return pushRequest;
	}

	public GitLabPushRequest mergePushRequest() {
		GitLabPushRequest pushRequest = new GitLabPushRequestBuilder().withRef("refs/heads/master")
				.withBefore("e8b9327c9704e308949f9d31dd0fae6abfac3798").withAfter(COMMIT_E5).withCheckoutSha(COMMIT_E5)
				.addCommit(COMMIT_74).addCommit("ab569fa9c51fa80d6509b277a6b587faf8e7cb72").addCommit(COMMIT_E5)
				.build();
		return pushRequest;

		// and afterwards the "delete branch" request comes in
	}

	public GitLabPushRequest pushNewTagRequest() {
		GitLabPushRequest pushRequest = new GitLabPushRequestBuilder().withRef("refs/tags/test-tag-2")
				.withBefore(NO_COMMIT_HASH).withAfter(COMMIT_64).withCheckoutSha(COMMIT_64).addCommit(COMMIT_64).build();
		return pushRequest;
	}

	public GitLabPushRequest deleteBranchRequest() {
		GitLabPushRequest pushRequest = new GitLabPushRequestBuilder().withRef("refs/heads/test-branch-delete-1")
				.withBefore("784c5ca7814aa7ea1913ae8e64187c31322946f0").withAfter(NO_COMMIT_HASH).withCheckoutSha(null)
				.build();
		return pushRequest;
	}

}
