package com.dabsquared.gitlabjenkins.testhelpers;

import com.dabsquared.gitlabjenkins.GitLabPushRequest;

public class CommitBuilder {

	public static GitLabPushRequest.Commit buildWithDefaults() {
		return new CommitBuilder().withCommitSha("4bf0fcd937085dc2f69dcbe31f2ef960ec9ca7eb").build();
	}

	private GitLabPushRequest.Commit commit;

	public CommitBuilder() {
		commit = new GitLabPushRequest.Commit();
		commit.setAuthor(new GitLabPushRequest.User());
		commit.getAuthor().setName("author name");
		commit.getAuthor().setEmail("author@example.com");
		commit.setTimestamp("2015-11-12T07:49:09+11:00");
	}

	public CommitBuilder withCommitSha(String commitSha) {
		commit.setId(commitSha);
		commit.setUrl("http://gitlabserver.example.com/test-group/test-repo/commit/" + commitSha);
		return this;
	}

	public GitLabPushRequest.Commit build() {
		return commit;
	}

}
