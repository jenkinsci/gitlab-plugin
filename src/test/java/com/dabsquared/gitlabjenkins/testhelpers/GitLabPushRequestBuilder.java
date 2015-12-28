package com.dabsquared.gitlabjenkins.testhelpers;

import com.dabsquared.gitlabjenkins.models.Commit;
import com.dabsquared.gitlabjenkins.models.request.GitLabPushRequest;

import java.util.ArrayList;

public class GitLabPushRequestBuilder {

	public static GitLabPushRequest buildWithDefaults() {
		return new GitLabPushRequestBuilder().withBasicValues().build();
	}

	private GitLabPushRequest pushRequest;

	public GitLabPushRequestBuilder() {
		pushRequest = new GitLabPushRequest();
		pushRequest.setUser_id(123);
		pushRequest.setUser_name("admin@example");
		pushRequest.setProject_id(345);
		pushRequest.setRepository(RepositoryBuilder.buildWithDefaults());
		pushRequest.setCommits(new ArrayList<Commit>());
	}

	public GitLabPushRequestBuilder withBasicValues() {
		withBefore("2bf4170829aedd706d7485d40091a01637b9abf4");
		withAfter("c04c8822d1df397fb7e6dd3dd133018a0af567a8");
		withCheckoutSha("c04c8822d1df397fb7e6dd3dd133018a0af567a8");
		withRef("refs/heads/master");
		addCommit("c04c8822d1df397fb7e6dd3dd133018a0af567a8");
		return this;
	}

	public GitLabPushRequestBuilder withBefore(String beforeSha) {
		pushRequest.setBefore(beforeSha);
		return this;
	}

	public GitLabPushRequestBuilder withAfter(String afterSha) {
		pushRequest.setAfter(afterSha);
		return this;
	}

	public GitLabPushRequestBuilder withCheckoutSha(String checkoutSha) {
		pushRequest.setCheckout_sha(checkoutSha);
		return this;
	}

	public GitLabPushRequestBuilder withRef(String ref) {
		pushRequest.setRef(ref);
		return this;
	}

	public GitLabPushRequestBuilder addCommit(String commitSha) {
		pushRequest.getCommits().add(new CommitBuilder().withCommitSha(commitSha).build());
		return this;
	}

	public GitLabPushRequest build() {
		pushRequest.setTotal_commits_count(pushRequest.getCommits().size());
		return pushRequest;
	}

}
